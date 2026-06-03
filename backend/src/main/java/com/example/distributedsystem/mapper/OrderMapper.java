package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.OrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {
    @Insert("INSERT INTO orders(order_no, user_id, total_amount, status, receiver_name, receiver_phone, user_address, paid_at, carrier, tracking_no) VALUES(#{orderNo}, #{userId}, #{totalAmount}, #{status}, #{receiverName}, #{receiverPhone}, #{userAddress}, #{paidAt}, #{carrier}, #{trackingNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderEntity order);

    @Select("SELECT id, order_no, user_id, total_amount, status, receiver_name, receiver_phone, user_address, paid_at, carrier, tracking_no, logistics_status, delivered_at, completed_at, reviewed, created_at FROM orders WHERE user_id = #{userId} ORDER BY id DESC")
    List<OrderEntity> findByUserId(Long userId);

    @Select("SELECT id, order_no, user_id, total_amount, status, receiver_name, receiver_phone, user_address, paid_at, carrier, tracking_no, logistics_status, delivered_at, completed_at, reviewed, created_at FROM orders ORDER BY id DESC")
    List<OrderEntity> findAll();

    @Select("SELECT id, order_no, user_id, total_amount, status, receiver_name, receiver_phone, user_address, paid_at, carrier, tracking_no, logistics_status, delivered_at, completed_at, reviewed, created_at FROM orders WHERE id = #{orderId} AND user_id = #{userId}")
    OrderEntity findByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Update("UPDATE orders SET status = #{status}, paid_at = CURRENT_TIMESTAMP, carrier = #{carrier}, tracking_no = #{trackingNo}, logistics_status = 'PAID' WHERE id = #{orderId} AND user_id = #{userId} AND status = 'CREATED'")
    int markPaid(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("status") String status, @Param("carrier") String carrier, @Param("trackingNo") String trackingNo);

    @Update("UPDATE orders SET status = #{status}, logistics_status = #{logisticsStatus}, delivered_at = #{deliveredAt} WHERE id = #{orderId} AND user_id = #{userId}")
    int updateLogistics(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("status") String status, @Param("logisticsStatus") String logisticsStatus, @Param("deliveredAt") java.time.LocalDateTime deliveredAt);

    @Update("UPDATE orders SET status = 'COMPLETED', logistics_status = 'SIGNED', completed_at = CURRENT_TIMESTAMP WHERE id = #{orderId} AND user_id = #{userId} AND status = 'DELIVERED'")
    int complete(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Update("UPDATE orders SET reviewed = 1 WHERE id = #{orderId} AND user_id = #{userId}")
    int markReviewed(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Update("UPDATE orders SET status = 'CANCELLED' WHERE id = #{orderId} AND user_id = #{userId} AND status = 'CREATED' AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    int cancelIfExpired(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Update("UPDATE orders SET status = 'CANCELLED' WHERE user_id = #{userId} AND status = 'CREATED' AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    int cancelExpiredByUser(@Param("userId") Long userId);
}
