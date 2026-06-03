package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.Inventory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper {
    @Insert("INSERT INTO inventory(product_id, stock) VALUES(#{productId}, #{stock})")
    int insert(Inventory inventory);

    @Select("SELECT id, product_id, stock FROM inventory WHERE product_id = #{productId}")
    Inventory findByProductId(Long productId);

    @Update("UPDATE inventory SET stock = stock - #{quantity} WHERE product_id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE inventory SET stock = #{stock}, updated_at = CURRENT_TIMESTAMP WHERE product_id = #{productId}")
    int updateStock(@Param("productId") Long productId, @Param("stock") Integer stock);
}
