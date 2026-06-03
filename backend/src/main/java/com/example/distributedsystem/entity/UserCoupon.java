package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private String code;
    private String couponCode;
    private String title;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private Long categoryId;
    private Long productId;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
}
