package com.example.distributedsystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductCreateRequest {
    @NotBlank
    private String name;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @NotNull
    private Integer stock;

    private Long categoryId;

    private String description;

    private String coverImage;

    private String carouselImages;

    private String tag;

    private String status;

    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private LocalDateTime seckillStartAt;
    private LocalDateTime seckillEndAt;
}
