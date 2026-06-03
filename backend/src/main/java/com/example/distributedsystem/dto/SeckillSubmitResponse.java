package com.example.distributedsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeckillSubmitResponse {
    private String requestId;
    private String status;
    private String message;
}
