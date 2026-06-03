package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.ProductReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductReviewMapper {
    @Insert("INSERT INTO product_review(product_id, username, avatar_url, rating, content) VALUES(#{productId}, #{username}, #{avatarUrl}, #{rating}, #{content})")
    int insert(ProductReview review);

    @Select("SELECT id, product_id, username, avatar_url, rating, content, created_at FROM product_review WHERE product_id = #{productId} ORDER BY created_at DESC, id DESC")
    List<ProductReview> findByProductId(Long productId);

    @Select("SELECT COUNT(*) FROM product_review")
    long countAll();
}
