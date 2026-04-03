package com.example.distributedsystem.service;

import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public String register(User user) {
        User exists = userMapper.findByUsername(user.getUsername());
        if (exists != null) {
            return "用户名已存在";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return "注册成功";
    }

    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return "用户不存在";
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return "密码错误";
        }
        return "登录成功";
    }
}
