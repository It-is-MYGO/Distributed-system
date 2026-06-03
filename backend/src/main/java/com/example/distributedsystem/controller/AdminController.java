package com.example.distributedsystem.controller;

import com.example.distributedsystem.entity.OrderEntity;
import com.example.distributedsystem.entity.Coupon;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.mapper.CouponMapper;
import com.example.distributedsystem.mapper.OrderMapper;
import com.example.distributedsystem.mapper.UserMapper;
import com.example.distributedsystem.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ProductService productService;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final CouponMapper couponMapper;

    public AdminController(ProductService productService, UserMapper userMapper, OrderMapper orderMapper, CouponMapper couponMapper) {
        this.productService = productService;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.couponMapper = couponMapper;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        var products = productService.list();
        var users = userMapper.findAll();
        var orders = orderMapper.findAll();
        BigDecimal paidAmount = orders.stream()
                .filter(order -> !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                .map(OrderEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var topProducts = products.stream()
                .sorted(Comparator.comparing(product -> -safeSales(product)))
                .limit(8)
                .toList();
        return ResponseEntity.ok(Map.of(
                "stats", Map.of(
                        "productCount", products.size(),
                        "userCount", users.size(),
                        "orderCount", orders.size(),
                        "paidAmount", paidAmount
                ),
                "products", products,
                "topProducts", topProducts,
                "users", users,
                "orders", orders,
                "coupons", couponMapper.findActive()
        ));
    }

    @PostMapping("/coupon")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        coupon.setStatus(coupon.getStatus() == null ? "ACTIVE" : coupon.getStatus());
        couponMapper.insert(coupon);
        return ResponseEntity.ok(coupon);
    }

    private int safeSales(Product product) {
        return product.getSalesCount() == null ? 0 : product.getSalesCount();
    }
}
