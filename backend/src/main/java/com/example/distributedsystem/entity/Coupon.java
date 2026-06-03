package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Coupon {
    private Long id;
    private String code;
    private String title;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private Long categoryId;
    private Long productId;
    private String status;
    private LocalDateTime createdAt;
}
