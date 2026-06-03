package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderEntity {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private String receiverName;
    private String receiverPhone;
    private String userAddress;
    private LocalDateTime paidAt;
    private String carrier;
    private String trackingNo;
    private String logisticsStatus;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private Boolean reviewed;
    private LocalDateTime createdAt;
}
