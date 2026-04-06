package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.AuthRequest;
import com.example.distributedsystem.dto.AuthResponse;
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
            return new AuthResponse("用户名已存在", null);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse("注册成功", token);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            return new AuthResponse("用户不存在", null);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse("密码错误", null);
        }
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse("登录成功", token);
    }

    public User getCurrentUser(String username) {
        return userMapper.findByUsername(username);
    }
}
