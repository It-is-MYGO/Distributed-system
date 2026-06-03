package com.example.distributedsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartUpdateRequest {
    @NotNull
    private Long cartItemId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
