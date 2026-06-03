package com.example.distributedsystem.service;

import com.example.distributedsystem.entity.Category;
import com.example.distributedsystem.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public Category create(Category category) {
        if (category.getCategoryLevel() == null) {
            category.setCategoryLevel(1);
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getCategoryRank() == null) {
            category.setCategoryRank(0);
        }
        category.setIsDeleted(false);
        categoryMapper.insert(category);
        return category;
    }

    public List<Category> list() {
        return categoryMapper.findAll();
    }

    public List<Category> tree() {
        List<Category> categories = categoryMapper.findAll();
        Map<Long, Category> byId = categories.stream().collect(Collectors.toMap(Category::getId, category -> category));
        List<Category> roots = new java.util.ArrayList<>();
        for (Category category : categories) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                roots.add(category);
            } else {
                Category parent = byId.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new java.util.ArrayList<>());
                    }
                    parent.getChildren().add(category);
                } else {
                    roots.add(category);
                }
            }
        }
        return roots;
    }

    public Category getById(Long id) {
        return categoryMapper.findById(id);
    }
}
