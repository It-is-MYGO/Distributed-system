package com.example.distributedsystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderReviewRequest {
    @Min(1)
    @Max(5)
    private Integer rating = 5;

    @NotBlank
    private String content;
}
