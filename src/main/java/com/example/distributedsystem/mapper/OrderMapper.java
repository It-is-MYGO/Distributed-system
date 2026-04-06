package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.OrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {
    @Insert("INSERT INTO orders(user_id, total_amount, status) VALUES(#{userId}, #{totalAmount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderEntity order);

    @Select("SELECT id, user_id, total_amount, status FROM orders WHERE user_id = #{userId} ORDER BY id DESC")
    List<OrderEntity> findByUserId(Long userId);
}
