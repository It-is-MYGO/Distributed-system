package com.example.distributedsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String receiverName;
    private String receiverPhone;
    private String address;
    private String couponCode;
    private List<String> couponCodes;
}
