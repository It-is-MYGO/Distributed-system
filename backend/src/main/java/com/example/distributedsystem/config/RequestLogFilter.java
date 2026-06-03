package com.example.distributedsystem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);
    private final AtomicLong counter = new AtomicLong(0);

    @Value("${app.instance-id:${server.port}}")
    private String instanceId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        long current = counter.incrementAndGet();
        if (request.getRequestURI().startsWith("/api/")) {
            log.info("instance={} count={} method={} uri={} status={}",
                    instanceId, current, request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
}
