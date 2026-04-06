package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.OrderCreateRequest;
import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.entity.OrderEntity;
import com.example.distributedsystem.entity.OrderItem;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.mapper.InventoryMapper;
import com.example.distributedsystem.mapper.OrderItemMapper;
import com.example.distributedsystem.mapper.OrderMapper;
import com.example.distributedsystem.mapper.ProductMapper;
import com.example.distributedsystem.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderService(
            UserMapper userMapper,
            ProductMapper productMapper,
            InventoryMapper inventoryMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper
    ) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public OrderEntity createOrder(String username, OrderCreateRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Product product = productMapper.findById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        Inventory inventory = inventoryMapper.findByProductId(product.getId());
        if (inventory == null || inventory.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("库存不足");
        }

        int updatedRows = inventoryMapper.deductStock(product.getId(), request.getQuantity());
        if (updatedRows <= 0) {
            throw new IllegalArgumentException("库存扣减失败");
        }

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setTotalAmount(totalAmount);
        order.setStatus("CREATED");
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(product.getPrice());
        orderItemMapper.insert(item);
        return order;
    }

    public List<OrderEntity> myOrders(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return orderMapper.findByUserId(user.getId());
    }
}
