package com.example.distributedsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillTransactionLog {
    private Long id;
    private String requestId;
    private String username;
    private Long productId;
    private Integer quantity;
    private String status;
    private Boolean redisDeducted;
    private Boolean dbDeducted;
    private String orderNo;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
