package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderEntity {
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
}
