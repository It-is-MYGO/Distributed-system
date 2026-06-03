package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShoppingCartItem {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String productName;
    private String productCoverImage;
    private BigDecimal unitPrice;
    private Long categoryId;
}
