package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.OrderCreateRequest;
import com.example.distributedsystem.dto.OrderReviewRequest;
import com.example.distributedsystem.dto.CartCheckoutRequest;
import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.entity.OrderEntity;
import com.example.distributedsystem.entity.OrderItem;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.entity.ProductReview;
import com.example.distributedsystem.entity.ShoppingCartItem;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.entity.UserCoupon;
import com.example.distributedsystem.dto.CartPreviewResponse;
import com.example.distributedsystem.mapper.InventoryMapper;
import com.example.distributedsystem.mapper.CouponMapper;
import com.example.distributedsystem.mapper.OrderItemMapper;
import com.example.distributedsystem.mapper.OrderMapper;
import com.example.distributedsystem.mapper.ProductMapper;
import com.example.distributedsystem.mapper.ProductReviewMapper;
import com.example.distributedsystem.mapper.ShoppingCartMapper;
import com.example.distributedsystem.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ProductReviewMapper productReviewMapper;
    private final InventoryMapper inventoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final SnowflakeIdService snowflakeIdService;
    private final CouponMapper couponMapper;

    public OrderService(
            UserMapper userMapper,
            ProductMapper productMapper,
            ProductReviewMapper productReviewMapper,
            InventoryMapper inventoryMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            ShoppingCartMapper shoppingCartMapper,
            SnowflakeIdService snowflakeIdService,
            CouponMapper couponMapper
    ) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.productReviewMapper = productReviewMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.shoppingCartMapper = shoppingCartMapper;
        this.snowflakeIdService = snowflakeIdService;
        this.couponMapper = couponMapper;
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

        BigDecimal unitPrice = effectivePrice(product);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        OrderEntity order = new OrderEntity();
        order.setOrderNo(buildOrderNo());
        order.setUserId(user.getId());
        order.setTotalAmount(applyCoupons(totalAmount, couponCodes(request.getCouponCode(), request.getCouponCodes()), user.getId(), List.of(product)));
        order.setStatus("CREATED");
        fillReceiver(order, request.getReceiverName(), request.getReceiverPhone(), request.getAddress(), user);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductCoverImage(product.getCoverImage());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(unitPrice);
        orderItemMapper.insert(item);
        useCouponsIfPresent(couponCodes(request.getCouponCode(), request.getCouponCodes()), user.getId());
        return order;
    }

    @Transactional
    public OrderEntity checkoutCart(String username, CartCheckoutRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            throw new IllegalArgumentException("请选择要结算的购物车项");
        }

        List<ShoppingCartItem> cartItems = shoppingCartMapper.findByUserId(user.getId());
        Map<Long, ShoppingCartItem> cartItemMap = cartItems.stream()
                .filter(item -> request.getCartItemIds().contains(item.getId()))
                .collect(Collectors.toMap(ShoppingCartItem::getId, item -> item));
        if (cartItemMap.isEmpty()) {
            throw new IllegalArgumentException("购物车项不存在或已删除");
        }

        List<Long> productIds = cartItemMap.values().stream().map(ShoppingCartItem::getProductId).distinct().toList();
        List<Product> products = productMapper.findByIds(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, product -> product));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : cartItemMap.values()) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在: " + cartItem.getProductId());
            }
            Inventory inventory = inventoryMapper.findByProductId(product.getId());
            if (inventory == null || inventory.getStock() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("商品库存不足: " + product.getName());
            }
            totalAmount = totalAmount.add(effectivePrice(product).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNo(buildOrderNo());
        order.setUserId(user.getId());
        order.setTotalAmount(applyCoupons(totalAmount, couponCodes(request.getCouponCode(), request.getCouponCodes()), user.getId(), products));
        order.setStatus("CREATED");
        fillReceiver(order, request.getReceiverName(), request.getReceiverPhone(), request.getAddress(), user);
        orderMapper.insert(order);

        for (ShoppingCartItem cartItem : cartItemMap.values()) {
            Product product = productMap.get(cartItem.getProductId());
            int updatedRows = inventoryMapper.deductStock(product.getId(), cartItem.getQuantity());
            if (updatedRows <= 0) {
                throw new IllegalArgumentException("库存扣减失败: " + product.getName());
            }
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductCoverImage(product.getCoverImage());
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(effectivePrice(product));
            orderItemMapper.insert(item);
        }

        shoppingCartMapper.deleteBatchByIds(user.getId(), request.getCartItemIds());
        useCouponsIfPresent(couponCodes(request.getCouponCode(), request.getCouponCodes()), user.getId());
        return order;
    }

    public List<OrderEntity> myOrders(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        orderMapper.cancelExpiredByUser(user.getId());
        List<OrderEntity> orders = orderMapper.findByUserId(user.getId());
        orders.forEach(order -> refreshOrderProgress(order, user));
        return orderMapper.findByUserId(user.getId());
    }

    public OrderEntity detail(String username, Long orderId) {
        User user = requireUser(username);
        orderMapper.cancelIfExpired(orderId, user.getId());
        OrderEntity order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        refreshOrderProgress(order, user);
        order = orderMapper.findByIdAndUserId(orderId, user.getId());
        return order;
    }

    @Transactional
    public OrderEntity pay(String username, Long orderId) {
        User user = requireUser(username);
        OrderEntity order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        orderMapper.cancelIfExpired(orderId, user.getId());
        order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("订单已超过15分钟支付时限，已自动作废");
        }
        if (!"CREATED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("订单已支付或不可重复支付");
        }
        int updatedRows = orderMapper.markPaid(orderId, user.getId(), "PAID", "顺丰速运", buildTrackingNo(orderId));
        if (updatedRows <= 0) {
            throw new IllegalArgumentException("订单已支付或不可重复支付");
        }
        orderItemMapper.findByOrderId(orderId).forEach(item -> productMapper.increaseSales(item.getProductId(), item.getQuantity()));
        return orderMapper.findByIdAndUserId(orderId, user.getId());
    }

    public java.util.List<OrderItem> items(String username, Long orderId) {
        User user = requireUser(username);
        OrderEntity order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return orderItemMapper.findByOrderId(orderId);
    }

    @Transactional
    public OrderEntity receive(String username, Long orderId) {
        User user = requireUser(username);
        OrderEntity order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        refreshOrderProgress(order, user);
        order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("订单尚未到货，暂不能签收");
        }
        orderMapper.complete(orderId, user.getId());
        return orderMapper.findByIdAndUserId(orderId, user.getId());
    }

    @Transactional
    public OrderEntity review(String username, Long orderId, OrderReviewRequest request) {
        User user = requireUser(username);
        OrderEntity order = orderMapper.findByIdAndUserId(orderId, user.getId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("订单完成后才能评价");
        }
        if (Boolean.TRUE.equals(order.getReviewed())) {
            throw new IllegalArgumentException("该订单已评价");
        }
        addReviews(order, user, request.getRating(), request.getContent());
        orderMapper.markReviewed(orderId, user.getId());
        return orderMapper.findByIdAndUserId(orderId, user.getId());
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
        List<Long> productIds = cartItems.stream().map(ShoppingCartItem::getProductId).distinct().toList();
        List<Product> products = productMapper.findByIds(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, product -> product));
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ShoppingCartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("商品不存在: " + cartItem.getProductId());
            }
            cartItem.setProductName(product.getName());
            cartItem.setProductCoverImage(product.getCoverImage());
            BigDecimal unitPrice = effectivePrice(product);
            cartItem.setUnitPrice(unitPrice);
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        CartPreviewResponse response = new CartPreviewResponse();
        response.setItems(cartItems);
        response.setTotalAmount(totalAmount);
        return response;
    }

    private String buildOrderNo() {
        return "ORD" + snowflakeIdService.nextId();
    }

    private String buildTrackingNo(Long orderId) {
        return "SF" + snowflakeIdService.nextId() + String.format("%04d", orderId % 10000);
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

    private void refreshOrderProgress(OrderEntity order, User user) {
        if (order == null) {
            return;
        }
        String status = String.valueOf(order.getStatus()).toUpperCase();
        if (("PAID".equals(status) || "SHIPPED".equals(status)) && order.getPaidAt() != null) {
            long minutes = Duration.between(order.getPaidAt(), LocalDateTime.now()).toMinutes();
            if (minutes >= 2 && !"DELIVERED".equals(status)) {
                orderMapper.updateLogistics(order.getId(), user.getId(), "DELIVERED", "DELIVERED", LocalDateTime.now());
            } else if (minutes >= 1 && "PAID".equals(status)) {
                orderMapper.updateLogistics(order.getId(), user.getId(), "SHIPPED", "IN_TRANSIT", null);
            }
        }
        if ("COMPLETED".equals(status)
                && !Boolean.TRUE.equals(order.getReviewed())
                && order.getCompletedAt() != null
                && Duration.between(order.getCompletedAt(), LocalDateTime.now()).toDays() >= 7) {
            addReviews(order, user, 5, "系统默认好评：用户超过一周未评价，默认五星好评。");
            orderMapper.markReviewed(order.getId(), user.getId());
        }
    }

    private void addReviews(OrderEntity order, User user, Integer rating, String content) {
        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
        for (OrderItem item : items) {
            ProductReview review = new ProductReview();
            review.setProductId(item.getProductId());
            review.setUsername(user.getNickName() == null || user.getNickName().isBlank() ? user.getUsername() : user.getNickName());
            review.setAvatarUrl(user.getAvatarUrl());
            review.setRating(rating == null ? 5 : rating);
            review.setContent(content);
            productReviewMapper.insert(review);
        }
    }

    private BigDecimal applyCoupon(BigDecimal amount, String couponCode, Long userId, Long productId, Long categoryId) {
        BigDecimal payable = amount == null ? BigDecimal.ZERO : amount;
        String code = couponCode == null ? "" : couponCode.trim().toUpperCase();
        if (code.isBlank()) {
            return payable.setScale(2, RoundingMode.HALF_UP);
        }
        UserCoupon coupon = couponMapper.findUsableByCode(userId, code);
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券不存在、已使用或未发放给当前用户");
        }
        boolean productMatched = coupon.getProductId() == null || coupon.getProductId().equals(productId);
        boolean categoryMatched = coupon.getCategoryId() == null || coupon.getCategoryId().equals(categoryId);
        if (!productMatched || !categoryMatched || payable.compareTo(coupon.getThresholdAmount()) < 0) {
            throw new IllegalArgumentException("优惠券不满足使用条件");
        }
        BigDecimal discount = coupon.getDiscountAmount();
        return payable.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void useCouponIfPresent(String couponCode, Long userId) {
        String code = couponCode == null ? "" : couponCode.trim().toUpperCase();
        if (code.isBlank()) {
            return;
        }
        UserCoupon coupon = couponMapper.findUsableByCode(userId, code);
        if (coupon == null || couponMapper.markUsed(coupon.getId(), userId) <= 0) {
            throw new IllegalArgumentException("优惠券核销失败");
        }
    }

    private List<String> couponCodes(String couponCode, List<String> couponCodes) {
        List<String> codes = new ArrayList<>();
        if (couponCode != null && !couponCode.isBlank()) {
            codes.add(couponCode);
        }
        if (couponCodes != null) {
            codes.addAll(couponCodes);
        }
        return codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase())
                .distinct()
                .toList();
    }

    private BigDecimal applyCoupons(BigDecimal amount, List<String> codes, Long userId, List<Product> products) {
        BigDecimal payable = amount == null ? BigDecimal.ZERO : amount;
        if (codes == null || codes.isEmpty()) {
            return payable.setScale(2, RoundingMode.HALF_UP);
        }
        List<UserCoupon> coupons = loadAndValidateCoupons(codes, userId, payable, products);
        BigDecimal discount = coupons.stream()
                .map(UserCoupon::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return payable.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private List<UserCoupon> loadAndValidateCoupons(List<String> codes, Long userId, BigDecimal payable, List<Product> products) {
        List<UserCoupon> coupons = new ArrayList<>();
        Set<String> scopes = new HashSet<>();
        for (String code : codes) {
            UserCoupon coupon = couponMapper.findUsableByCode(userId, code);
            if (coupon == null) {
                throw new IllegalArgumentException("优惠券不存在、已使用或未发放给当前用户");
            }
            String scope = couponScope(coupon);
            if (!scopes.add(scope)) {
                throw new IllegalArgumentException("同类型优惠券不能重复叠加");
            }
            if (payable.compareTo(coupon.getThresholdAmount()) < 0 || !couponMatches(coupon, products)) {
                throw new IllegalArgumentException("优惠券不满足使用条件");
            }
            coupons.add(coupon);
        }
        return coupons;
    }

    private String couponScope(UserCoupon coupon) {
        if (coupon.getProductId() != null) {
            return "PRODUCT:" + coupon.getProductId();
        }
        if (coupon.getCategoryId() != null) {
            return "CATEGORY:" + coupon.getCategoryId();
        }
        return "PLATFORM";
    }

    private boolean couponMatches(UserCoupon coupon, List<Product> products) {
        if (coupon.getProductId() == null && coupon.getCategoryId() == null) {
            return true;
        }
        return products.stream().anyMatch(product -> {
            boolean productMatched = coupon.getProductId() == null || coupon.getProductId().equals(product.getId());
            boolean categoryMatched = coupon.getCategoryId() == null || coupon.getCategoryId().equals(product.getCategoryId());
            return productMatched && categoryMatched;
        });
    }

    private void useCouponsIfPresent(List<String> codes, Long userId) {
        if (codes == null || codes.isEmpty()) {
            return;
        }
        for (String code : codes) {
            UserCoupon coupon = couponMapper.findUsableByCode(userId, code);
            if (coupon == null || couponMapper.markUsed(coupon.getId(), userId) <= 0) {
                throw new IllegalArgumentException("优惠券核销失败");
            }
        }
    }

    private User requireUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private void fillReceiver(OrderEntity order, String receiverName, String receiverPhone, String address, User user) {
        String resolvedName = receiverName == null || receiverName.isBlank()
                ? (user.getNickName() == null || user.getNickName().isBlank() ? user.getUsername() : user.getNickName())
                : receiverName.trim();
        String resolvedAddress = address == null || address.isBlank() ? user.getAddress() : address.trim();
        if (resolvedName == null || resolvedName.isBlank()) {
            throw new IllegalArgumentException("请填写收货人姓名");
        }
        if (receiverPhone == null || receiverPhone.isBlank()) {
            throw new IllegalArgumentException("请填写手机号");
        }
        if (resolvedAddress == null || resolvedAddress.isBlank()) {
            throw new IllegalArgumentException("请填写收货地址");
        }
        order.setReceiverName(resolvedName);
        order.setReceiverPhone(receiverPhone.trim());
        order.setUserAddress(resolvedAddress);
        order.setPaidAt(null);
        order.setCarrier("待支付后生成");
        order.setTrackingNo("待支付后生成");
    }
}
