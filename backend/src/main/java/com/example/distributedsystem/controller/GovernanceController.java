package com.example.distributedsystem.controller;

import com.example.distributedsystem.config.TrafficGovernanceFilter;
import com.example.distributedsystem.dto.GovernanceConfigUpdateRequest;
import com.example.distributedsystem.entity.GovernanceConfig;
import com.example.distributedsystem.service.GovernanceConfigService;
import com.example.distributedsystem.service.NacosConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {
    private final GovernanceConfigService configService;
    private final NacosConfigService nacosConfigService;
    private final TrafficGovernanceFilter trafficGovernanceFilter;

    public GovernanceController(GovernanceConfigService configService, NacosConfigService nacosConfigService, TrafficGovernanceFilter trafficGovernanceFilter) {
        this.configService = configService;
        this.nacosConfigService = nacosConfigService;
        this.trafficGovernanceFilter = trafficGovernanceFilter;
    }

    @GetMapping("/config")
    public ResponseEntity<List<GovernanceConfig>> listConfig() {
        return ResponseEntity.ok(configService.list());
    }

    @PostMapping("/config")
    public ResponseEntity<GovernanceConfig> updateConfig(@Valid @RequestBody GovernanceConfigUpdateRequest request) {
        GovernanceConfig config = configService.update(request.getKey(), request.getValue());
        trafficGovernanceFilter.reset();
        nacosConfigService.publish(configService.list());
        return ResponseEntity.ok(config);
    }

    @GetMapping("/service-registry")
    public ResponseEntity<Map<String, Object>> serviceRegistry() {
        Map<String, Object> view = configService.registryView();
        return ResponseEntity.ok(new java.util.LinkedHashMap<>() {{
            putAll(view);
            put("nacosConfig", nacosConfigService.metadata());
        }});
    }

    @PostMapping("/nacos/publish")
    public ResponseEntity<Map<String, Object>> publishNacos() {
        return ResponseEntity.ok(nacosConfigService.publish(configService.list()));
    }

    @GetMapping("/nacos/config")
    public ResponseEntity<Map<String, String>> pullNacos() {
        return ResponseEntity.ok(nacosConfigService.pull());
    }

    @GetMapping("/traffic/status")
    public ResponseEntity<Map<String, Object>> trafficStatus() {
        return ResponseEntity.ok(trafficGovernanceFilter.status());
    }

    @PostMapping("/traffic/reset")
    public ResponseEntity<Map<String, Object>> resetTraffic() {
        trafficGovernanceFilter.reset();
        return ResponseEntity.ok(trafficGovernanceFilter.status());
    }
}
