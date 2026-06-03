package com.example.distributedsystem.service;

import com.example.distributedsystem.entity.GovernanceConfig;
import com.example.distributedsystem.mapper.GovernanceConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GovernanceConfigService {
    private final GovernanceConfigMapper governanceConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Value("${app.instance-id:local}")
    private String instanceId;

    @Value("${server.port:8080}")
    private String serverPort;

    public GovernanceConfigService(GovernanceConfigMapper governanceConfigMapper) {
        this.governanceConfigMapper = governanceConfigMapper;
    }

    public List<GovernanceConfig> list() {
        List<GovernanceConfig> configs = governanceConfigMapper.findAll();
        configs.forEach(config -> cache.put(config.getConfigKey(), config.getConfigValue()));
        return configs;
    }

    public GovernanceConfig update(String key, String value) {
        if (governanceConfigMapper.updateValue(key, value) <= 0) {
            throw new IllegalArgumentException("治理配置不存在：" + key);
        }
        cache.put(key, value);
        return governanceConfigMapper.findByKey(key);
    }

    public String get(String key, String defaultValue) {
        GovernanceConfig config = governanceConfigMapper.findByKey(key);
        if (config == null) {
            return defaultValue;
        }
        cache.put(key, config.getConfigValue());
        return config.getConfigValue();
    }

    public boolean bool(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public int integer(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public Map<String, Object> registryView() {
        return Map.of(
                "nacosServer", get("nacos.server-addr", "http://nacos:8848"),
                "gateway", "http://localhost/api",
                "currentInstance", Map.of("instanceId", instanceId, "port", serverPort),
                "registeredServices", List.of(
                        Map.of("serviceName", "mall-service", "instanceId", "app-1", "address", "app1:8081", "gatewayRoute", "/api/**"),
                        Map.of("serviceName", "mall-service", "instanceId", "app-2", "address", "app2:8082", "gatewayRoute", "/api/**")
                )
        );
    }
}
