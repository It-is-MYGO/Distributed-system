package com.example.distributedsystem.service;

import com.example.distributedsystem.entity.Coupon;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.entity.UserCoupon;
import com.example.distributedsystem.mapper.CouponMapper;
import com.example.distributedsystem.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponService {
    private final CouponMapper couponMapper;
    private final UserMapper userMapper;

    public CouponService(CouponMapper couponMapper, UserMapper userMapper) {
        this.couponMapper = couponMapper;
        this.userMapper = userMapper;
    }

    public List<UserCoupon> mine(String username) {
        User user = requireUser(username);
        return couponMapper.findByUserId(user.getId());
    }

    public List<Coupon> activeCoupons() {
        return couponMapper.findActive();
    }

    public List<UserCoupon> claim(String username, String code) {
        User user = requireUser(username);
        Coupon coupon = couponMapper.findActiveByCode(code == null ? "" : code.trim().toUpperCase());
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券不存在或已下线");
        }
        couponMapper.issue(user.getId(), coupon.getId());
        return couponMapper.findByUserId(user.getId());
    }

    private User requireUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }
}
