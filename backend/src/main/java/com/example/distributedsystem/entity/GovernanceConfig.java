package com.example.distributedsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GovernanceConfig {
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
