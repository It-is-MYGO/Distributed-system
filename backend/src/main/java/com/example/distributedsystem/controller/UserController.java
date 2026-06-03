package com.example.distributedsystem.controller;

import com.example.distributedsystem.dto.AuthRequest;
import com.example.distributedsystem.dto.AuthResponse;
import com.example.distributedsystem.dto.UserUpdateRequest;
import com.example.distributedsystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest user) {
        return ResponseEntity.ok(userService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.getCurrentUser(username));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(Authentication authentication, @Valid @RequestBody UserUpdateRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.updateCurrentUser(username, request));
    }
}
