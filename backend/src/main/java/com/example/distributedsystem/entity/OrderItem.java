package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productCoverImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
}
