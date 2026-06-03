package com.example.distributedsystem.service;

import com.example.distributedsystem.dto.OrderCreateRequest;
import com.example.distributedsystem.dto.SeckillOrderMessage;
import com.example.distributedsystem.dto.SeckillSubmitResponse;
import com.example.distributedsystem.entity.Inventory;
import com.example.distributedsystem.entity.OrderEntity;
import com.example.distributedsystem.entity.SeckillTransactionLog;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.mapper.InventoryMapper;
import com.example.distributedsystem.mapper.SeckillTransactionLogMapper;
import com.example.distributedsystem.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SeckillOrderService {
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_LOCK_KEY_PREFIX = "seckill:once:";

    private final UserMapper userMapper;
    private final InventoryMapper inventoryMapper;
    private final OrderService orderService;
    private final SnowflakeIdService snowflakeIdService;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SeckillTransactionLogMapper transactionLogMapper;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public SeckillOrderService(
            UserMapper userMapper,
            InventoryMapper inventoryMapper,
            OrderService orderService,
            SnowflakeIdService snowflakeIdService,
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            SeckillTransactionLogMapper transactionLogMapper,
            ObjectMapper objectMapper,
            @Value("${app.kafka.seckill-topic:seckill-orders}") String topicName
    ) {
        this.userMapper = userMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderService = orderService;
        this.snowflakeIdService = snowflakeIdService;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionLogMapper = transactionLogMapper;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public SeckillSubmitResponse submit(String username, OrderCreateRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        String userLockKey = USER_LOCK_KEY_PREFIX + user.getId() + ":" + request.getProductId();
        Boolean firstSubmit = redisTemplate.opsForValue().setIfAbsent(userLockKey, "1", Duration.ofMinutes(30));
        if (!Boolean.TRUE.equals(firstSubmit)) {
            throw new IllegalArgumentException("同一用户同一商品只能秒杀一次");
        }

        String stockKey = STOCK_KEY_PREFIX + request.getProductId();
        initStockCacheIfAbsent(request.getProductId(), stockKey);
        Long stockAfterDeduct = redisTemplate.opsForValue().decrement(stockKey, quantity);
        if (stockAfterDeduct == null || stockAfterDeduct < 0) {
            redisTemplate.opsForValue().increment(stockKey, quantity);
            redisTemplate.delete(userLockKey);
            throw new IllegalArgumentException("秒杀库存不足");
        }

        String requestId = String.valueOf(snowflakeIdService.nextId());
        insertTransactionLog(requestId, username, request.getProductId(), quantity, "PENDING", true, false, null);

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId(requestId);
        message.setUsername(username);
        message.setProductId(request.getProductId());
        message.setQuantity(quantity);
        message.setReceiverName(request.getReceiverName());
        message.setReceiverPhone(request.getReceiverPhone());
        message.setAddress(request.getAddress());

        try {
            kafkaTemplate.send(topicName, message.getRequestId(), toJson(message));
            updateTransactionLog(message.getRequestId(), "QUEUED", true, false, null, null);
            return new SeckillSubmitResponse(message.getRequestId(), "QUEUED", "秒杀请求已进入 Kafka 队列，订单将异步创建");
        } catch (Exception e) {
            OrderEntity order = orderService.createOrder(username, request);
            updateTransactionLog(message.getRequestId(), "SUCCESS", true, true, order.getOrderNo(), null);
            return new SeckillSubmitResponse(message.getRequestId(), "CREATED_SYNC", "Kafka 暂不可用，已降级为同步创建订单");
        }
    }

    @KafkaListener(topics = "${app.kafka.seckill-topic:seckill-orders}", groupId = "${spring.kafka.consumer.group-id:ds-seckill-group}")
    public void consume(String payload) {
        SeckillOrderMessage message;
        try {
            message = objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (Exception ignored) {
            return;
        }
        OrderCreateRequest request = new OrderCreateRequest();
        request.setProductId(message.getProductId());
        request.setQuantity(message.getQuantity());
        request.setReceiverName(message.getReceiverName());
        request.setReceiverPhone(message.getReceiverPhone());
        request.setAddress(message.getAddress());
        try {
            updateTransactionLog(message.getRequestId(), "PROCESSING", true, false, null, null);
            OrderEntity order = orderService.createOrder(message.getUsername(), request);
            updateTransactionLog(message.getRequestId(), "SUCCESS", true, true, order.getOrderNo(), null);
        } catch (Exception e) {
            redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + message.getProductId(), message.getQuantity());
            User user = userMapper.findByUsername(message.getUsername());
            if (user != null) {
                redisTemplate.delete(USER_LOCK_KEY_PREFIX + user.getId() + ":" + message.getProductId());
            }
            updateTransactionLog(message.getRequestId(), "COMPENSATED", false, false, null, e.getMessage());
        }
    }

    private void insertTransactionLog(String requestId, String username, Long productId, Integer quantity,
                                      String status, boolean redisDeducted, boolean dbDeducted, String errorMessage) {
        SeckillTransactionLog log = new SeckillTransactionLog();
        log.setRequestId(requestId);
        log.setUsername(username);
        log.setProductId(productId);
        log.setQuantity(quantity);
        log.setStatus(status);
        log.setRedisDeducted(redisDeducted);
        log.setDbDeducted(dbDeducted);
        log.setErrorMessage(errorMessage);
        transactionLogMapper.insert(log);
    }

    private void updateTransactionLog(String requestId, String status, boolean redisDeducted, boolean dbDeducted,
                                      String orderNo, String errorMessage) {
        SeckillTransactionLog log = transactionLogMapper.findByRequestId(requestId);
        if (log == null) {
            return;
        }
        log.setStatus(status);
        log.setRedisDeducted(redisDeducted);
        log.setDbDeducted(dbDeducted);
        log.setOrderNo(orderNo == null ? log.getOrderNo() : orderNo);
        log.setErrorMessage(errorMessage);
        transactionLogMapper.update(log);
    }

    private void initStockCacheIfAbsent(Long productId, String stockKey) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            return;
        }
        Inventory inventory = inventoryMapper.findByProductId(productId);
        if (inventory == null) {
            throw new IllegalArgumentException("商品库存不存在");
        }
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(inventory.getStock()), Duration.ofMinutes(30));
    }

    private String toJson(SeckillOrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("秒杀消息序列化失败", e);
        }
    }
}
