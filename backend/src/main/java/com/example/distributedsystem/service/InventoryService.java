package com.example.distributedsystem.service;

import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.mapper.InventoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    public Inventory createInventory(Long productId, Integer stock) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setStock(stock);
        inventoryMapper.insert(inventory);
        return inventory;
    }

    public Inventory findByProductId(Long productId) {
        return inventoryMapper.findByProductId(productId);
    }

    @Transactional
    public int deductStock(Long productId, Integer quantity) {
        return inventoryMapper.deductStock(productId, quantity);
    }
}
