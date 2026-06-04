package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.AuthRequest;
import com.example.distributedsystem.dto.AuthResponse;
import com.example.distributedsystem.dto.UserUpdateRequest;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.mapper.UserMapper;
import com.example.distributedsystem.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRequest request) {
        User exists = userMapper.findByUsername(request.getUsername());
        if (exists != null) {
            return new AuthResponse("用户名已存在", null, null);
        }
        String role = "USER";

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setNickName(request.getNickName() == null || request.getNickName().isBlank() ? request.getUsername() : request.getNickName());
        user.setIntroduceSign(request.getIntroduceSign());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(defaultAvatarUrl(user.getUsername(), user.getNickName()));
        user.setLockedFlag(false);
        userMapper.insert(user);
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse("注册成功", token, role);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            return new AuthResponse("用户不存在", null, null);
        }
        if (user.getLockedFlag() != null && user.getLockedFlag()) {
            return new AuthResponse("用户已被锁定", null, null);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse("密码错误", null, null);
        }
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse("登录成功", token, user.getRole());
    }

    public User getCurrentUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public User updateCurrentUser(String username, UserUpdateRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (request.getNickName() != null) {
            user.setNickName(request.getNickName());
        }
        if (request.getIntroduceSign() != null) {
            user.setIntroduceSign(request.getIntroduceSign());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getVerifyCode() == null || !request.getVerifyCode().trim().equals("888888")) {
                throw new IllegalArgumentException("验证码不正确，演示验证码为 888888");
            }
            if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("原密码不正确");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userMapper.updatePassword(user);
        }
        userMapper.update(user);
        user.setPassword(null);
        return user;
    }

    private String defaultAvatarUrl(String username, String nickName) {
        String seed = (nickName == null || nickName.isBlank()) ? username : nickName;
        return "https://api.dicebear.com/8.x/thumbs/svg?seed=" + seed;
    }
}
