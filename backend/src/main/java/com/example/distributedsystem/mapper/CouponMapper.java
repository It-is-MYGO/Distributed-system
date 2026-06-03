package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.Coupon;
import com.example.distributedsystem.entity.UserCoupon;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponMapper {
    @Insert("INSERT INTO coupon(code, title, threshold_amount, discount_amount, category_id, product_id, status) VALUES(#{code}, #{title}, #{thresholdAmount}, #{discountAmount}, #{categoryId}, #{productId}, #{status})")
    int insert(Coupon coupon);

    @Select("SELECT id, code, title, threshold_amount, discount_amount, category_id, product_id, status, created_at FROM coupon WHERE code = #{code} AND status = 'ACTIVE'")
    Coupon findActiveByCode(String code);

    @Select("SELECT id, code, title, threshold_amount, discount_amount, category_id, product_id, status, created_at FROM coupon WHERE status = 'ACTIVE' ORDER BY discount_amount DESC")
    List<Coupon> findActive();

    @Select("SELECT COUNT(*) FROM coupon WHERE code = #{code}")
    int countByCode(String code);

    @Insert("INSERT INTO user_coupon(user_id, coupon_id, status) VALUES(#{userId}, #{couponId}, 'UNUSED')")
    int issue(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Select("""
            SELECT uc.id, uc.user_id, uc.coupon_id, c.code, c.code AS coupon_code, c.title, c.threshold_amount, c.discount_amount,
                   c.category_id, c.product_id, uc.status, uc.issued_at, uc.used_at
            FROM user_coupon uc
            JOIN coupon c ON uc.coupon_id = c.id
            WHERE uc.user_id = #{userId}
            ORDER BY uc.status DESC, c.discount_amount DESC, uc.id DESC
            """)
    List<UserCoupon> findByUserId(Long userId);

    @Select("""
            SELECT uc.id, uc.user_id, uc.coupon_id, c.code, c.code AS coupon_code, c.title, c.threshold_amount, c.discount_amount,
                   c.category_id, c.product_id, uc.status, uc.issued_at, uc.used_at
            FROM user_coupon uc
            JOIN coupon c ON uc.coupon_id = c.id
            WHERE uc.user_id = #{userId} AND c.code = #{code} AND uc.status = 'UNUSED' AND c.status = 'ACTIVE'
            ORDER BY uc.id LIMIT 1
            """)
    UserCoupon findUsableByCode(@Param("userId") Long userId, @Param("code") String code);

    @Update("UPDATE user_coupon SET status = 'USED', used_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId} AND status = 'UNUSED'")
    int markUsed(@Param("id") Long id, @Param("userId") Long userId);
}
