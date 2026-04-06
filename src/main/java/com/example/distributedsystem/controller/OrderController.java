package com.example.distributedsystem.controller;

import com.example.distributedsystem.dto.OrderCreateRequest;
import com.example.distributedsystem.entity.OrderEntity;
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

    @GetMapping("/mine")
    public ResponseEntity<List<OrderEntity>> mine(Authentication authentication) {
        return ResponseEntity.ok(orderService.myOrders(authentication.getName()));
    }
}
