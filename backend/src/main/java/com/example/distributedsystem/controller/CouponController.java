package com.example.distributedsystem.controller;

import com.example.distributedsystem.entity.Coupon;
import com.example.distributedsystem.entity.UserCoupon;
import com.example.distributedsystem.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<Coupon>> active() {
        return ResponseEntity.ok(couponService.activeCoupons());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<UserCoupon>> mine(Authentication authentication) {
        return ResponseEntity.ok(couponService.mine(authentication.getName()));
    }

    @PostMapping("/claim")
    public ResponseEntity<List<UserCoupon>> claim(Authentication authentication, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(couponService.claim(authentication.getName(), body.get("code")));
    }
}
