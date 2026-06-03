package com.example.distributedsystem.config;

import com.example.distributedsystem.service.GovernanceConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TrafficGovernanceFilter extends OncePerRequestFilter {
    private final GovernanceConfigService configService;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger failureCounter = new AtomicInteger(0);
    private volatile long circuitOpenUntil = 0L;

    public TrafficGovernanceFilter(GovernanceConfigService configService) {
        this.configService = configService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/governance/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = Instant.now().toEpochMilli();
        if (configService.bool("traffic.circuit-breaker.enabled", true) && now < circuitOpenUntil) {
            writeGovernanceResponse(response, 503, "服务熔断中，请稍后重试", "CIRCUIT_OPEN");
            return;
        }

        if (configService.bool("traffic.rate-limit.enabled", true)) {
            int permits = Math.max(1, configService.integer("traffic.rate-limit.permits-per-second", 35));
            TokenBucket bucket = buckets.computeIfAbsent(path, ignored -> new TokenBucket(permits));
            bucket.resize(permits);
            if (!bucket.tryAcquire()) {
                writeGovernanceResponse(response, 429, "当前访问过于频繁，已触发限流", "RATE_LIMITED");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
            if (response.getStatus() >= 500) {
                recordFailure(path);
            } else {
                failureCounter.updateAndGet(value -> Math.max(0, value - 1));
            }
        } catch (ServletException | IOException | RuntimeException ex) {
            recordFailure(path);
            throw ex;
        }
    }

    private void recordFailure(String path) {
        int threshold = Math.max(1, configService.integer("traffic.circuit-breaker.failure-threshold", 8));
        int failures = failureCounter.incrementAndGet();
        if (failures >= threshold) {
            int openSeconds = Math.max(1, configService.integer("traffic.circuit-breaker.open-seconds", 20));
            circuitOpenUntil = Instant.now().plusSeconds(openSeconds).toEpochMilli();
            failureCounter.set(0);
        }
        String degradePrefix = configService.get("traffic.degrade.path-prefix", "/api/product/search");
        if (configService.bool("traffic.degrade.enabled", true) && path.startsWith(degradePrefix)) {
            circuitOpenUntil = Math.max(circuitOpenUntil, Instant.now().plusSeconds(3).toEpochMilli());
        }
    }

    private void writeGovernanceResponse(HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    public Map<String, Object> status() {
        return Map.of(
                "rateLimitEnabled", configService.bool("traffic.rate-limit.enabled", true),
                "permitsPerSecond", configService.integer("traffic.rate-limit.permits-per-second", 35),
                "circuitBreakerEnabled", configService.bool("traffic.circuit-breaker.enabled", true),
                "failureCount", failureCounter.get(),
                "circuitOpen", Instant.now().toEpochMilli() < circuitOpenUntil,
                "circuitOpenUntil", circuitOpenUntil
        );
    }

    public void reset() {
        buckets.clear();
        failureCounter.set(0);
        circuitOpenUntil = 0L;
    }

    private static class TokenBucket {
        private int capacity;
        private int tokens;
        private long lastRefillMillis;

        TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillMillis = Instant.now().toEpochMilli();
        }

        synchronized void resize(int newCapacity) {
            if (newCapacity != capacity) {
                capacity = newCapacity;
                tokens = Math.min(tokens, capacity);
            }
        }

        synchronized boolean tryAcquire() {
            long now = Instant.now().toEpochMilli();
            if (now - lastRefillMillis >= 1000) {
                tokens = capacity;
                lastRefillMillis = now;
            }
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }
    }
}
