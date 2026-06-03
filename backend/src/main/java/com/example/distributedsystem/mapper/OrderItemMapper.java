package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderItemMapper {
    @Insert("INSERT INTO order_item(order_id, product_id, product_name, product_cover_image, quantity, unit_price) VALUES(#{orderId}, #{productId}, #{productName}, #{productCoverImage}, #{quantity}, #{unitPrice})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderItem orderItem);

    @Select("SELECT id, order_id, product_id, product_name, product_cover_image, quantity, unit_price FROM order_item WHERE order_id = #{orderId}")
    java.util.List<OrderItem> findByOrderId(Long orderId);
}
