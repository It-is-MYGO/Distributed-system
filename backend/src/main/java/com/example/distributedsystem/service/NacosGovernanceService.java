package com.example.distributedsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class NacosGovernanceService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NacosGovernanceService.class);
    private static final String SERVICE_NAME = "mall-service";
    private static final String DATA_ID = "mall-governance.properties";
    private static final String GROUP = "DEFAULT_GROUP";

    private final GovernanceConfigService governanceConfigService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String nacosServer;
    private final String instanceId;
    private final int serverPort;
    private String instanceIp = "127.0.0.1";

    public NacosGovernanceService(
            GovernanceConfigService governanceConfigService,
            @Value("${app.nacos.server-addr:localhost:8848}") String nacosServer,
            @Value("${app.instance-id:local}") String instanceId,
            @Value("${server.port:8080}") int serverPort
    ) {
        this.governanceConfigService = governanceConfigService;
        this.nacosServer = nacosServer.startsWith("http") ? nacosServer : "http://" + nacosServer;
        this.instanceId = instanceId;
        this.serverPort = serverPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            instanceIp = InetAddress.getLocalHost().getHostAddress();
            registerInstance();
            publishDefaultConfig();
            syncConfigFromNacos();
        } catch (Exception ex) {
            log.warn("Nacos governance bootstrap skipped: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 8000)
    public void heartbeat() {
        try {
            String beat = """
                    {"serviceName":"%s","ip":"%s","port":%d,"cluster":"DEFAULT","metadata":{"instanceId":"%s"}}
                    """.formatted(SERVICE_NAME, instanceIp, serverPort, instanceId);
            String url = nacosServer + "/nacos/v1/ns/instance/beat?serviceName=" + encode(SERVICE_NAME)
                    + "&ip=" + encode(instanceIp)
                    + "&port=" + serverPort
                    + "&beat=" + encode(beat);
            restTemplate.put(URI.create(url), null);
        } catch (Exception ex) {
            log.debug("Nacos heartbeat failed: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 10000)
    public void syncConfigFromNacos() {
        try {
            String url = nacosServer + "/nacos/v1/cs/configs?dataId=" + encode(DATA_ID) + "&group=" + encode(GROUP);
            String content = restTemplate.getForObject(URI.create(url), String.class);
            if (content == null || content.isBlank()) {
                return;
            }
            Arrays.stream(content.split("\\R"))
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#") && line.contains("="))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        try {
                            governanceConfigService.update(parts[0].trim(), parts[1].trim());
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ex) {
            log.debug("Nacos config sync failed: {}", ex.getMessage());
        }
    }

    private void registerInstance() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("serviceName", SERVICE_NAME);
        form.add("ip", instanceIp);
        form.add("port", String.valueOf(serverPort));
        form.add("ephemeral", "true");
        form.add("metadata", "{\"instanceId\":\"" + instanceId + "\"}");
        restTemplate.postForObject(nacosServer + "/nacos/v1/ns/instance", form, String.class);
    }

    private void publishDefaultConfig() {
        String content = """
                traffic.rate-limit.enabled=true
                traffic.rate-limit.permits-per-second=35
                traffic.circuit-breaker.enabled=true
                traffic.circuit-breaker.failure-threshold=8
                traffic.circuit-breaker.open-seconds=20
                traffic.degrade.enabled=true
                traffic.degrade.path-prefix=/api/product/search
                """;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("dataId", DATA_ID);
        form.add("group", GROUP);
        form.add("content", content);
        restTemplate.postForObject(nacosServer + "/nacos/v1/cs/configs", form, String.class);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
