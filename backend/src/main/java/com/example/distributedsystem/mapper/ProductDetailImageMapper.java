package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.ProductDetailImage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductDetailImageMapper {
    @Insert("INSERT INTO product_detail_image(product_id, image_url, image_type, sort_order) VALUES(#{productId}, #{imageUrl}, #{imageType}, #{sortOrder})")
    int insert(ProductDetailImage image);

    @Select("SELECT id, product_id, image_url, image_type, sort_order, created_at FROM product_detail_image WHERE product_id = #{productId} ORDER BY sort_order, id")
    List<ProductDetailImage> findByProductId(Long productId);

    @Select("SELECT COUNT(*) FROM product_detail_image")
    long countAll();
}
