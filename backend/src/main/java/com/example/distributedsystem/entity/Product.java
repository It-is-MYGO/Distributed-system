package com.example.distributedsystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private Long categoryId;
    private String description;
    private String coverImage;
    private String carouselImages;
    private String tag;
    private Integer salesCount;
    private Integer categorySalesRank;
    private String couponText;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private LocalDateTime seckillStartAt;
    private LocalDateTime seckillEndAt;
    private String status;
    private Integer stock;
    private LocalDateTime createdAt;
}
