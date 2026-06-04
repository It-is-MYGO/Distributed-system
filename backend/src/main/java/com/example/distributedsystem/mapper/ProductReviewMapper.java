package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.ProductReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductReviewMapper {
    @Insert("INSERT INTO product_review(product_id, order_id, username, avatar_url, rating, content) VALUES(#{productId}, #{orderId}, #{username}, #{avatarUrl}, #{rating}, #{content})")
    int insert(ProductReview review);

    @Select("SELECT id, product_id, order_id, username, avatar_url, rating, content, followup_content, followup_at, created_at FROM product_review WHERE product_id = #{productId} ORDER BY created_at DESC, id DESC")
    List<ProductReview> findByProductId(Long productId);

    @Update("UPDATE product_review SET followup_content = #{content}, followup_at = CURRENT_TIMESTAMP WHERE order_id = #{orderId} AND product_id = #{productId} AND (followup_content IS NULL OR followup_content = '')")
    int addFollowup(@Param("orderId") Long orderId, @Param("productId") Long productId, @Param("content") String content);

    @Select("SELECT COUNT(*) FROM product_review")
    long countAll();
}
