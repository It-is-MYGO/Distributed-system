package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    public LocalDateTime getPaymentExpiresAt() {
        return createdAt == null ? null : createdAt.plusMinutes(15);
    }

    public Long getPaymentExpiresAtMillis() {
        return createdAt == null ? null : createdAt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public Long getPaymentSecondsLeft() {
        if (createdAt == null || !"CREATED".equalsIgnoreCase(status)) {
            return 0L;
        }
        long seconds = Duration.between(LocalDateTime.now(), createdAt.plusMinutes(15)).getSeconds();
        return Math.max(0L, seconds);
    }
}
