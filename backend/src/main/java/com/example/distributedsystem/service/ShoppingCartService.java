package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.CartItemRequest;
import com.example.distributedsystem.dto.CartUpdateRequest;
import com.example.distributedsystem.dto.CartPreviewResponse;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.entity.ShoppingCartItem;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.mapper.ProductMapper;
import com.example.distributedsystem.mapper.ShoppingCartMapper;
import com.example.distributedsystem.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShoppingCartService {
    private static final int MAX_CART_ITEMS = 100;

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ShoppingCartMapper shoppingCartMapper;

    public ShoppingCartService(UserMapper userMapper,
                               ProductMapper productMapper,
                               ShoppingCartMapper shoppingCartMapper) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.shoppingCartMapper = shoppingCartMapper;
    }

    @Transactional
    public ShoppingCartItem add(String username, CartItemRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Product product = productMapper.findById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        ShoppingCartItem existing = shoppingCartMapper.findByUserIdAndProductId(user.getId(), product.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            shoppingCartMapper.updateQuantity(existing);
            enrichItem(existing, product);
            return existing;
        }
        int count = shoppingCartMapper.countByUserId(user.getId());
        if (count >= MAX_CART_ITEMS) {
            throw new IllegalArgumentException("购物车商品数量已达上限");
        }
        ShoppingCartItem item = new ShoppingCartItem();
        item.setUserId(user.getId());
        item.setProductId(product.getId());
        item.setQuantity(request.getQuantity());
        item.setIsDeleted(false);
        shoppingCartMapper.insert(item);
        enrichItem(item, product);
        return item;
    }

    @Transactional
    public ShoppingCartItem update(String username, CartUpdateRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        ShoppingCartItem item = shoppingCartMapper.findById(request.getCartItemId());
        if (item == null || !user.getId().equals(item.getUserId())) {
            throw new IllegalArgumentException("购物车项不存在或无权限");
        }
        item.setQuantity(request.getQuantity());
        int updated = shoppingCartMapper.updateQuantity(item);
        if (updated <= 0) {
            throw new IllegalArgumentException("更新购物车失败");
        }
        Product product = productMapper.findById(item.getProductId());
        enrichItem(item, product);
        return item;
    }

    @Transactional
    public boolean delete(String username, Long cartItemId) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return shoppingCartMapper.deleteByIdAndUserId(cartItemId, user.getId()) > 0;
    }

    @Transactional
    public int deleteBatch(String username, java.util.List<Long> cartItemIds) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return 0;
        }
        return shoppingCartMapper.deleteBatchByIds(user.getId(), cartItemIds);
    }

    public List<ShoppingCartItem> list(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        List<ShoppingCartItem> items = shoppingCartMapper.findByUserId(user.getId());
        if (items.isEmpty()) {
            return items;
        }
        Map<Long, Product> productMap = productMapper.findByIds(items.stream().map(ShoppingCartItem::getProductId).distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        for (ShoppingCartItem item : items) {
            enrichItem(item, productMap.get(item.getProductId()));
        }
        return items;
    }

    public CartPreviewResponse previewCheckout(String username, java.util.List<Long> cartItemIds) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要结算的购物车项");
        }
        List<ShoppingCartItem> cartItems = shoppingCartMapper.findByUserId(user.getId()).stream()
                .filter(item -> cartItemIds.contains(item.getId()))
                .collect(Collectors.toList());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("购物车项不存在或已删除");
        }
        List<Long> productIds = cartItems.stream().map(ShoppingCartItem::getProductId).distinct().collect(Collectors.toList());
        List<Product> products = productMapper.findByIds(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, product -> product));
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在: " + cartItem.getProductId());
            }
            enrichItem(cartItem, product);
            totalAmount = totalAmount.add(effectivePrice(product).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        CartPreviewResponse response = new CartPreviewResponse();
        response.setItems(cartItems);
        response.setTotalAmount(totalAmount);
        return response;
    }

    private void enrichItem(ShoppingCartItem item, Product product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName());
        item.setProductCoverImage(product.getCoverImage());
        item.setUnitPrice(effectivePrice(product));
        item.setCategoryId(product.getCategoryId());
    }

    private BigDecimal effectivePrice(Product product) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        if (product.getSeckillPrice() != null
                && product.getSeckillStartAt() != null
                && product.getSeckillEndAt() != null
                && !now.isBefore(product.getSeckillStartAt())
                && now.isBefore(product.getSeckillEndAt())) {
            return product.getSeckillPrice();
        }
        return product.getOriginalPrice() == null ? product.getPrice() : product.getOriginalPrice();
    }
}
