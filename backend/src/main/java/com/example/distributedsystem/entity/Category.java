package com.example.distributedsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Category {
    private Long id;
    private Integer categoryLevel;
    private Long parentId;
    private String name;
    private Integer categoryRank;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Category> children;
}
