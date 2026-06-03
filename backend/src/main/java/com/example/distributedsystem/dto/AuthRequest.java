package com.example.distributedsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String nickName;

    private String introduceSign;

    private String address;

    private String role;
}
