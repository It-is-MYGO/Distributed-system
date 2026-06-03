package com.example.distributedsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GovernanceConfigUpdateRequest {
    @NotBlank
    private String key;

    @NotBlank
    private String value;
}
