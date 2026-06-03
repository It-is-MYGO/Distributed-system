package com.example.distributedsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String role;
    private String nickName;
    private String introduceSign;
    private String address;
    private String avatarUrl;
    private Boolean lockedFlag;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
}
