package com.example.distributedsystem.controller;

import com.example.distributedsystem.dto.CartCheckoutRequest;
import com.example.distributedsystem.dto.CartItemRequest;
import com.example.distributedsystem.dto.CartPreviewResponse;
import com.example.distributedsystem.dto.CartUpdateRequest;
import com.example.distributedsystem.entity.ShoppingCartItem;
import com.example.distributedsystem.service.ShoppingCartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@Validated
public class ShoppingCartController {
    private final ShoppingCartService shoppingCartService;

    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @PostMapping
    public ResponseEntity<ShoppingCartItem> add(Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(shoppingCartService.add(authentication.getName(), request));
    }

    @PutMapping
    public ResponseEntity<ShoppingCartItem> update(Authentication authentication, @Valid @RequestBody CartUpdateRequest request) {
        return ResponseEntity.ok(shoppingCartService.update(authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
        boolean removed = shoppingCartService.delete(authentication.getName(), id);
        if (!removed) {
            return ResponseEntity.badRequest().body("购物车项删除失败");
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteBatch(Authentication authentication, @RequestParam java.util.List<Long> ids) {
        int removed = shoppingCartService.deleteBatch(authentication.getName(), ids);
        if (removed <= 0) {
            return ResponseEntity.badRequest().body("批量删除购物车项失败或未找到");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/preview")
    public ResponseEntity<CartPreviewResponse> preview(Authentication authentication, @RequestBody CartCheckoutRequest request) {
        return ResponseEntity.ok(shoppingCartService.previewCheckout(authentication.getName(), request.getCartItemIds()));
    }

    @GetMapping
    public ResponseEntity<List<ShoppingCartItem>> list(Authentication authentication) {
        return ResponseEntity.ok(shoppingCartService.list(authentication.getName()));
    }
}
