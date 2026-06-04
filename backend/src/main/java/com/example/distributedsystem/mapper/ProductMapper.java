package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.Product;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Mapper
public interface ProductMapper {
    @Insert("INSERT INTO product(name, price, category_id, description, cover_image, carousel_images, tag, sales_count, original_price, seckill_price, seckill_start_at, seckill_end_at, status) VALUES(#{name}, #{price}, #{categoryId}, #{description}, #{coverImage}, #{carouselImages}, #{tag}, #{salesCount}, #{originalPrice}, #{seckillPrice}, #{seckillStartAt}, #{seckillEndAt}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Select("SELECT id, name, price, category_id, description, cover_image, carousel_images, tag, sales_count, original_price, seckill_price, seckill_start_at, seckill_end_at, status, created_at FROM product WHERE id = #{id}")
    Product findById(Long id);

    @Select({"<script>",
            "SELECT id, name, price, category_id, description, cover_image, carousel_images, tag, sales_count, original_price, seckill_price, seckill_start_at, seckill_end_at, status, created_at FROM product",
            "WHERE id IN",
            "<foreach item='item' collection='ids' open='(' separator=',' close=')'>#{item}</foreach>",
            "</script>"})
    List<Product> findByIds(@Param("ids") List<Long> ids);

    @Select("SELECT id, name, price, category_id, description, cover_image, carousel_images, tag, sales_count, original_price, seckill_price, seckill_start_at, seckill_end_at, status, created_at FROM product ORDER BY id")
    List<Product> findAll();

    @Select({"<script>",
            "SELECT p.id, p.name, p.price, p.category_id, p.description, p.cover_image, p.carousel_images, p.tag, p.sales_count, p.original_price, p.seckill_price, p.seckill_start_at, p.seckill_end_at, p.status, p.created_at FROM product p",
            "LEFT JOIN goods_category c ON p.category_id = c.id",
            "WHERE p.name LIKE CONCAT('%', #{keyword}, '%')",
            "OR p.description LIKE CONCAT('%', #{keyword}, '%')",
            "OR p.tag LIKE CONCAT('%', #{keyword}, '%')",
            "OR c.name LIKE CONCAT('%', #{keyword}, '%')",
            "ORDER BY p.id DESC LIMIT #{pageable.pageSize} OFFSET #{pageable.offset}",
            "</script>"})
    List<Product> searchByKeyword(@Param("keyword") String keyword, @Param("pageable") Pageable pageable);

    @Select({"<script>",
            "SELECT COUNT(*) FROM product p",
            "LEFT JOIN goods_category c ON p.category_id = c.id",
            "WHERE p.name LIKE CONCAT('%', #{keyword}, '%')",
            "OR p.description LIKE CONCAT('%', #{keyword}, '%')",
            "OR p.tag LIKE CONCAT('%', #{keyword}, '%')",
            "OR c.name LIKE CONCAT('%', #{keyword}, '%')",
            "</script>"})
    long countByKeyword(@Param("keyword") String keyword);

    @Select({"<script>",
            "SELECT id, name, price, category_id, description, cover_image, carousel_images, tag, sales_count, original_price, seckill_price, seckill_start_at, seckill_end_at, status, created_at FROM product",
            "ORDER BY id DESC LIMIT #{pageable.pageSize} OFFSET #{pageable.offset}",
            "</script>"})
    List<Product> findAllPaged(@Param("pageable") Pageable pageable);

    @Select("SELECT COUNT(*) FROM product")
    long countAll();

    @Select({"<script>",
            "SELECT id, name, price, category_id, description, cover_image, carousel_images, tag, sales_count, original_price, seckill_price, seckill_start_at, seckill_end_at, status, created_at FROM product",
            "WHERE category_id = #{categoryId}",
            "<if test='excludeIds != null and excludeIds.size() > 0'>",
            "AND id NOT IN",
            "<foreach item='item' collection='excludeIds' open='(' separator=',' close=')'>#{item}</foreach>",
            "</if>",
            "ORDER BY sales_count DESC, id DESC LIMIT #{limit}",
            "</script>"})
    List<Product> findByCategoryForRecommend(@Param("categoryId") Long categoryId, @Param("excludeIds") List<Long> excludeIds, @Param("limit") int limit);

    @org.apache.ibatis.annotations.Update("UPDATE product SET sales_count = sales_count + #{quantity} WHERE id = #{productId}")
    int increaseSales(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @org.apache.ibatis.annotations.Update("UPDATE product SET name=#{name}, price=#{price}, category_id=#{categoryId}, description=#{description}, cover_image=#{coverImage}, tag=#{tag}, original_price=#{originalPrice}, seckill_price=#{seckillPrice}, seckill_start_at=#{seckillStartAt}, seckill_end_at=#{seckillEndAt}, status=#{status} WHERE id=#{id}")
    int update(Product product);
}
