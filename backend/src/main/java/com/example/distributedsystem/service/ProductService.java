package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.ProductCreateRequest;
import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.entity.ProductDetailImage;
import com.example.distributedsystem.entity.ProductReview;
import com.example.distributedsystem.mapper.InventoryMapper;
import com.example.distributedsystem.mapper.CouponMapper;
import com.example.distributedsystem.mapper.ProductDetailImageMapper;
import com.example.distributedsystem.mapper.ProductMapper;
import com.example.distributedsystem.mapper.ProductReviewMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {
    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_LOCK_KEY_PREFIX = "lock:product:";
    private static final String NULL_PLACEHOLDER = "NULL";

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductDetailImageMapper productDetailImageMapper;
    private final ProductReviewMapper productReviewMapper;
    private final CouponMapper couponMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductService(
            ProductMapper productMapper,
            InventoryMapper inventoryMapper,
            ProductDetailImageMapper productDetailImageMapper,
            ProductReviewMapper productReviewMapper,
            CouponMapper couponMapper,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.productDetailImageMapper = productDetailImageMapper;
        this.productReviewMapper = productReviewMapper;
        this.couponMapper = couponMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Product create(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategoryId(request.getCategoryId());
        product.setDescription(request.getDescription());
        product.setCoverImage(request.getCoverImage());
        product.setCarouselImages(request.getCarouselImages());
        product.setTag(request.getTag());
        product.setSalesCount(0);
        product.setOriginalPrice(request.getOriginalPrice() == null ? request.getPrice() : request.getOriginalPrice());
        product.setSeckillPrice(request.getSeckillPrice());
        product.setSeckillStartAt(request.getSeckillStartAt());
        product.setSeckillEndAt(request.getSeckillEndAt());
        product.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        productMapper.insert(product);

        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setStock(request.getStock());
        inventoryMapper.insert(inventory);
        product.setStock(request.getStock());
        return product;
    }

    public List<Product> list() {
        List<Product> products = productMapper.findAll();
        decorateProducts(products);
        return products;
    }

    public Page<Product> search(String keyword, int page, int size, String orderBy) {
        Pageable pageable = PageRequest.of(page, size);
        List<Product> products;
        long total;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productMapper.searchByKeyword(keyword.trim(), pageable);
            total = productMapper.countByKeyword(keyword.trim());
        } else {
            products = productMapper.findAllPaged(pageable);
            total = productMapper.countAll();
        }

        // Apply sorting
        if ("new".equals(orderBy)) {
            products.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        } else if ("price".equals(orderBy)) {
            products.sort((a, b) -> {
                if (a.getPrice() == null && b.getPrice() == null) return 0;
                if (a.getPrice() == null) return 1;
                if (b.getPrice() == null) return -1;
                return a.getPrice().compareTo(b.getPrice());
            });
        }
        // default order is already by id desc

        decorateProducts(products);
        return new PageImpl<>(products, pageable, total);
    }

    public List<Product> recommendSimilar(Long categoryId, List<Long> excludeIds, int limit) {
        if (categoryId == null) {
            List<Product> products = productMapper.findAllPaged(PageRequest.of(0, Math.max(1, Math.min(limit, 12))));
            decorateProducts(products);
            return products;
        }
        List<Product> products = productMapper.findByCategoryForRecommend(categoryId, excludeIds == null ? List.of() : excludeIds, Math.max(1, Math.min(limit, 12)));
        if (products.size() < Math.min(limit, 12)) {
            List<Long> excluded = products.stream().map(Product::getId).collect(Collectors.toList());
            if (excludeIds != null) excluded.addAll(excludeIds);
            List<Product> fallback = productMapper.findAllPaged(PageRequest.of(0, Math.max(1, Math.min(limit, 12))));
            fallback.stream()
                    .filter(product -> !excluded.contains(product.getId()))
                    .limit(Math.max(0, Math.min(limit, 12) - products.size()))
                    .forEach(products::add);
        }
        decorateProducts(products);
        return products;
    }

    public Product getDetail(Long id) {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
        String cacheVal;
        try {
            cacheVal = redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            return loadProductFromDatabase(id);
        }
        if (NULL_PLACEHOLDER.equals(cacheVal)) {
            return null;
        }
        if (cacheVal != null && !cacheVal.isBlank()) {
            return toProduct(cacheVal);
        }

        String lockKey = PRODUCT_LOCK_KEY_PREFIX + id;
        boolean locked;
        try {
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 8, TimeUnit.SECONDS));
        } catch (Exception e) {
            return loadProductFromDatabase(id);
        }
        if (!locked) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String retryVal;
            try {
                retryVal = redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception e) {
                return loadProductFromDatabase(id);
            }
            if (NULL_PLACEHOLDER.equals(retryVal)) {
                return null;
            }
            if (retryVal != null && !retryVal.isBlank()) {
                return toProduct(retryVal);
            }
        }

        try {
            Product product = loadProductFromDatabase(id);
            if (product == null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER, 2, TimeUnit.MINUTES);
                } catch (Exception ignored) {
                }
                return null;
            }
            long ttlSeconds = 600 + ThreadLocalRandom.current().nextInt(0, 300);
            try {
                redisTemplate.opsForValue().set(cacheKey, toJson(product), ttlSeconds, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            return product;
        } finally {
            if (locked) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public List<ProductDetailImage> listImages(Long productId) {
        return productDetailImageMapper.findByProductId(productId);
    }

    public List<ProductReview> listReviews(Long productId) {
        return productReviewMapper.findByProductId(productId);
    }

    private Product loadProductFromDatabase(Long id) {
        Product product = productMapper.findById(id);
        if (product != null) {
            decorateProducts(List.of(product));
        }
        return product;
    }

    @Transactional
    public Product update(Long id, ProductCreateRequest request) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategoryId(request.getCategoryId());
        product.setDescription(request.getDescription());
        product.setCoverImage(request.getCoverImage());
        product.setTag(request.getTag());
        product.setOriginalPrice(request.getOriginalPrice() == null ? request.getPrice() : request.getOriginalPrice());
        product.setSeckillPrice(request.getSeckillPrice());
        product.setSeckillStartAt(request.getSeckillStartAt());
        product.setSeckillEndAt(request.getSeckillEndAt());
        product.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        productMapper.update(product);
        inventoryMapper.updateStock(id, request.getStock() == null ? 0 : request.getStock());
        redisTemplate.delete(PRODUCT_CACHE_KEY_PREFIX + id);
        product.setStock(request.getStock());
        return product;
    }

    private void decorateProducts(List<Product> products) {
        products.forEach(this::fillStock);
        products.forEach(this::applySeckillPrice);
        fillCategoryRank(products);
        fillCouponText(products);
    }

    private void applySeckillPrice(Product product) {
        if (product == null || product.getOriginalPrice() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (product.getSeckillPrice() != null
                && product.getSeckillStartAt() != null
                && product.getSeckillEndAt() != null
                && !now.isBefore(product.getSeckillStartAt())
                && now.isBefore(product.getSeckillEndAt())) {
            product.setPrice(product.getSeckillPrice());
        } else {
            product.setPrice(product.getOriginalPrice());
        }
    }

    private void fillCategoryRank(List<Product> products) {
        Map<Long, List<Product>> grouped = products.stream()
                .collect(Collectors.groupingBy(product -> product.getCategoryId() == null ? 0L : product.getCategoryId()));
        for (List<Product> group : grouped.values()) {
            List<Product> sorted = group.stream()
                    .sorted(Comparator.comparing(product -> -safeSales(product)))
                    .toList();
            for (int i = 0; i < sorted.size(); i++) {
                sorted.get(i).setCategorySalesRank(i + 1);
            }
        }
    }

    private void fillCouponText(List<Product> products) {
        var coupons = couponMapper.findActive();
        for (Product product : products) {
            coupons.stream()
                    .filter(coupon -> coupon.getProductId() == null || coupon.getProductId().equals(product.getId()))
                    .filter(coupon -> coupon.getCategoryId() == null || coupon.getCategoryId().equals(product.getCategoryId()))
                    .sorted(Comparator.comparing(coupon -> couponPriority(coupon.getProductId(), coupon.getCategoryId())))
                    .findFirst()
                    .ifPresent(coupon -> product.setCouponText(coupon.getThresholdAmount().signum() > 0
                            ? "券 满" + coupon.getThresholdAmount().stripTrailingZeros().toPlainString() + "减" + coupon.getDiscountAmount().stripTrailingZeros().toPlainString()
                            : "券 立减" + coupon.getDiscountAmount().stripTrailingZeros().toPlainString()));
        }
    }

    private int safeSales(Product product) {
        return product.getSalesCount() == null ? 0 : product.getSalesCount();
    }

    private int couponPriority(Long productId, Long categoryId) {
        if (productId != null) return 0;
        if (categoryId != null) return 1;
        return 2;
    }

    private void fillStock(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        Inventory inventory = inventoryMapper.findByProductId(product.getId());
        product.setStock(inventory == null ? 0 : inventory.getStock());
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
