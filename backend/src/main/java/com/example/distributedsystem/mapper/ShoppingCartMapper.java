package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    @Select("SELECT id, user_id, product_id, quantity, is_deleted, created_at, updated_at FROM shopping_cart_item WHERE user_id = #{userId} AND product_id = #{productId} AND is_deleted = 0")
    ShoppingCartItem findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("SELECT id, user_id, product_id, quantity, is_deleted, created_at, updated_at FROM shopping_cart_item WHERE id = #{id} AND is_deleted = 0")
    ShoppingCartItem findById(Long id);

    @Select("SELECT id, user_id, product_id, quantity, is_deleted, created_at, updated_at FROM shopping_cart_item WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY id DESC")
    List<ShoppingCartItem> findByUserId(Long userId);

    @Select("SELECT count(1) FROM shopping_cart_item WHERE user_id = #{userId} AND is_deleted = 0")
    int countByUserId(Long userId);

    @Insert("INSERT INTO shopping_cart_item(user_id, product_id, quantity, is_deleted) VALUES(#{userId}, #{productId}, #{quantity}, #{isDeleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShoppingCartItem shoppingCartItem);

    @Update("UPDATE shopping_cart_item SET quantity = #{quantity}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND user_id = #{userId}")
    int updateQuantity(ShoppingCartItem shoppingCartItem);

    @Delete("DELETE FROM shopping_cart_item WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Delete({"<script>",
            "DELETE FROM shopping_cart_item WHERE user_id = #{userId} AND id IN",
            "<foreach item='item' collection='ids' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"})
    int deleteBatchByIds(@Param("userId") Long userId, @Param("ids") java.util.List<Long> ids);
}
