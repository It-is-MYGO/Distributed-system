package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {
    @Insert("INSERT INTO goods_category(category_level, parent_id, name, category_rank, is_deleted) VALUES(#{categoryLevel}, #{parentId}, #{name}, #{categoryRank}, #{isDeleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Select("SELECT id, category_level, parent_id, name, category_rank, is_deleted, created_at, updated_at FROM goods_category WHERE is_deleted = 0 ORDER BY category_rank")
    List<Category> findAll();

    @Select("SELECT id, category_level, parent_id, name, category_rank, is_deleted, created_at, updated_at FROM goods_category WHERE id = #{id}")
    Category findById(Long id);
}
