package com.example.distributedsystem.controller;

import com.example.distributedsystem.dto.OrderCreateRequest;
import com.example.distributedsystem.dto.SeckillSubmitResponse;
import com.example.distributedsystem.service.SeckillOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillOrderService seckillOrderService;

    public SeckillController(SeckillOrderService seckillOrderService) {
        this.seckillOrderService = seckillOrderService;
    }

    @PostMapping("/order")
    public ResponseEntity<SeckillSubmitResponse> submit(Authentication authentication, @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(seckillOrderService.submit(authentication.getName(), request));
    }
}
