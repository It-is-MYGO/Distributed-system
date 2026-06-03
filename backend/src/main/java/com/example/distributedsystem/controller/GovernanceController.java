package com.example.distributedsystem.controller;

import com.example.distributedsystem.config.TrafficGovernanceFilter;
import com.example.distributedsystem.dto.GovernanceConfigUpdateRequest;
import com.example.distributedsystem.entity.GovernanceConfig;
import com.example.distributedsystem.service.GovernanceConfigService;
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
    private final TrafficGovernanceFilter trafficGovernanceFilter;

    public GovernanceController(GovernanceConfigService configService, TrafficGovernanceFilter trafficGovernanceFilter) {
        this.configService = configService;
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
        return ResponseEntity.ok(config);
    }

    @GetMapping("/service-registry")
    public ResponseEntity<Map<String, Object>> serviceRegistry() {
        return ResponseEntity.ok(configService.registryView());
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
