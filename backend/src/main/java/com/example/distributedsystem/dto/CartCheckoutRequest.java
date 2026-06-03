package com.example.distributedsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CartCheckoutRequest {
    @NotEmpty
    private List<Long> cartItemIds;

    private String receiverName;
    private String receiverPhone;
    private String address;
    private String couponCode;
    private List<String> couponCodes;
}
