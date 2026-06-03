package com.example.distributedsystem.dto;

import lombok.Data;

@Data
public class SeckillOrderMessage {
    private String requestId;
    private String username;
    private Long productId;
    private Integer quantity;
    private String receiverName;
    private String receiverPhone;
    private String address;
}
