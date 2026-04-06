package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.Product;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {
    @Insert("INSERT INTO product(name, price) VALUES(#{name}, #{price})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Select("SELECT id, name, price FROM product WHERE id = #{id}")
    Product findById(Long id);

    @Select("SELECT id, name, price FROM product ORDER BY id")
    List<Product> findAll();
}
