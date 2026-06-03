package com.example.distributedsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductDetailImage {
    private Long id;
    private Long productId;
    private String imageUrl;
    private String imageType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
