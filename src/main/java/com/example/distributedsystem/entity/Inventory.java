package com.example.distributedsystem.entity;

import lombok.Data;

@Data
public class Inventory {
    private Long id;
    private Long productId;
    private Integer stock;
}
