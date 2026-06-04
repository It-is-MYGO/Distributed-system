package com.example.distributedsystem.controller;

import com.example.distributedsystem.dto.CartCheckoutRequest;
import com.example.distributedsystem.dto.OrderCreateRequest;
import com.example.distributedsystem.dto.OrderReviewRequest;
import com.example.distributedsystem.entity.OrderEntity;
import com.example.distributedsystem.entity.OrderItem;
import com.example.distributedsystem.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderEntity> create(Authentication authentication, @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(orderService.createOrder(authentication.getName(), request));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderEntity> checkout(Authentication authentication, @Valid @RequestBody CartCheckoutRequest request) {
        return ResponseEntity.ok(orderService.checkoutCart(authentication.getName(), request));
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<java.util.List<OrderItem>> orderItems(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.items(authentication.getName(), orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderEntity> detail(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.detail(authentication.getName(), orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderEntity> pay(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.pay(authentication.getName(), orderId));
    }

    @PostMapping("/{orderId}/receive")
    public ResponseEntity<OrderEntity> receive(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.receive(authentication.getName(), orderId));
    }

    @PostMapping("/{orderId}/review")
    public ResponseEntity<OrderEntity> review(Authentication authentication, @PathVariable Long orderId, @Valid @RequestBody OrderReviewRequest request) {
        return ResponseEntity.ok(orderService.review(authentication.getName(), orderId, request));
    }

    @PostMapping("/{orderId}/followup")
    public ResponseEntity<OrderEntity> followup(Authentication authentication, @PathVariable Long orderId, @Valid @RequestBody OrderReviewRequest request) {
        return ResponseEntity.ok(orderService.followup(authentication.getName(), orderId, request));
    }

    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<OrderEntity> paymentStatus(Authentication authentication, @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.detail(authentication.getName(), orderId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<OrderEntity>> mine(Authentication authentication) {
        return ResponseEntity.ok(orderService.myOrders(authentication.getName()));
    }
}
