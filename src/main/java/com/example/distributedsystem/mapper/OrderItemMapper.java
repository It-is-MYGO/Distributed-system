package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface OrderItemMapper {
    @Insert("INSERT INTO order_item(order_id, product_id, quantity, unit_price) VALUES(#{orderId}, #{productId}, #{quantity}, #{unitPrice})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderItem orderItem);
}
