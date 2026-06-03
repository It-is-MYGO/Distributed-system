package com.example.distributedsystem.dto;

import com.example.distributedsystem.entity.ShoppingCartItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartPreviewResponse {
    private List<ShoppingCartItem> items;
    private BigDecimal totalAmount;
}
