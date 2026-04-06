package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.ProductCreateRequest;
import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.mapper.InventoryMapper;
import com.example.distributedsystem.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {
    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_LOCK_KEY_PREFIX = "lock:product:";
    private static final String NULL_PLACEHOLDER = "NULL";

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductService(
            ProductMapper productMapper,
            InventoryMapper inventoryMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Product create(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        productMapper.insert(product);

        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setStock(request.getStock());
        inventoryMapper.insert(inventory);
        return product;
    }

    public List<Product> list() {
        return productMapper.findAll();
    }

    public Product getDetail(Long id) {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
        String cacheVal = redisTemplate.opsForValue().get(cacheKey);
        if (NULL_PLACEHOLDER.equals(cacheVal)) {
            return null;
        }
        if (cacheVal != null && !cacheVal.isBlank()) {
            return toProduct(cacheVal);
        }

        String lockKey = PRODUCT_LOCK_KEY_PREFIX + id;
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 8, TimeUnit.SECONDS));
        if (!locked) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String retryVal = redisTemplate.opsForValue().get(cacheKey);
            if (NULL_PLACEHOLDER.equals(retryVal)) {
                return null;
            }
            if (retryVal != null && !retryVal.isBlank()) {
                return toProduct(retryVal);
            }
        }

        try {
            Product product = productMapper.findById(id);
            if (product == null) {
                // Penetration protection: cache empty result shortly.
                redisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER, 2, TimeUnit.MINUTES);
                return null;
            }
            // Avalanche protection: randomize TTL.
            long ttlSeconds = 600 + ThreadLocalRandom.current().nextInt(0, 300);
            redisTemplate.opsForValue().set(cacheKey, toJson(product), ttlSeconds, TimeUnit.SECONDS);
            return product;
        } finally {
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    private String toJson(Product product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("商品缓存序列化失败", e);
        }
    }

    private Product toProduct(String json) {
        try {
            return objectMapper.readValue(json, Product.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("商品缓存反序列化失败", e);
        }
    }
}
