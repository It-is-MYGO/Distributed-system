package com.example.distributedsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductReview {
    private Long id;
    private Long productId;
    private String username;
    private String avatarUrl;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
