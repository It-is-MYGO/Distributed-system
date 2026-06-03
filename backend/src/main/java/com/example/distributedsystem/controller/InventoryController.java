package com.example.distributedsystem.controller;

import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.service.InventoryService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> get(@PathVariable Long productId) {
        Inventory inventory = inventoryService.findByProductId(productId);
        if (inventory == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(inventory);
    }

    @PostMapping
    public ResponseEntity<Inventory> create(@RequestParam Long productId, @RequestParam @NotNull @Min(0) Integer stock) {
        return ResponseEntity.ok(inventoryService.createInventory(productId, stock));
    }

    @PostMapping("/{productId}/deduct")
    public ResponseEntity<?> deduct(@PathVariable Long productId, @RequestParam @NotNull @Min(1) Integer quantity) {
        int updated = inventoryService.deductStock(productId, quantity);
        if (updated <= 0) {
            return ResponseEntity.badRequest().body("库存扣减失败");
        }
        return ResponseEntity.ok().build();
    }
}
