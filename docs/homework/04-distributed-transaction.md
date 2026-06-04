# 作业四：分布式事务

## 一、作业要求

1. 在商品秒杀系统中，订单服务和库存服务是两个独立服务，分别有各自数据库。
2. 秒杀下单时，基于 Redis 实现库存预扣减，防超卖、限购。
3. 采用基于消息的一致性或 TCC 事务保障数据一致性。
4. 保证下单和库存扣减一致。
5. 保证订单支付和订单状态更新一致。

## 二、完成情况

已完成：Redis 预扣库存、防超卖、限购、Kafka 消息最终一致性、事务日志、失败补偿、下单和库存扣减一致、支付状态幂等更新。

说明：当前项目采用“基于消息的最终一致性”，未实现 TCC。题目允许二选一，因此满足要求。

## 三、事务场景设计

秒杀下单涉及以下数据：

```text
Redis:
  seckill:stock:{productId}         秒杀库存
  seckill:once:{userId}:{productId} 用户限购锁

MySQL:
  inventory                         商品库存
  orders                            订单主表
  order_item                        订单明细表
  seckill_transaction_log           秒杀事务日志

Kafka:
  seckill-orders                    秒杀订单消息队列
```

## 四、秒杀事务流程

```text
1. 用户提交秒杀请求
2. Redis 判断同一用户同一商品是否已经秒杀过
3. Redis 原子扣减秒杀库存
4. 写入秒杀事务日志 PENDING
5. 发送 Kafka 消息，状态变为 QUEUED
6. 消费端收到消息，创建订单
7. MySQL 原子扣减库存
8. 写入订单和订单明细
9. 成功后事务日志变为 SUCCESS
10. 失败时回补 Redis 库存，删除用户限购锁，状态变为 COMPENSATED
```

## 五、Redis 库存预扣

```java
Long stockAfterDeduct = redisTemplate.opsForValue().decrement(stockKey, quantity);
if (stockAfterDeduct == null || stockAfterDeduct < 0) {
    redisTemplate.opsForValue().increment(stockKey, quantity);
    redisTemplate.delete(userLockKey);
    throw new IllegalArgumentException("秒杀库存不足");
}
```

该操作在 Redis 中完成，速度快，适合高并发秒杀入口。

## 六、限购与幂等

```java
Boolean firstSubmit = redisTemplate.opsForValue()
    .setIfAbsent(userLockKey, "1", Duration.ofMinutes(30));

if (!Boolean.TRUE.equals(firstSubmit)) {
    throw new IllegalArgumentException("同一用户同一商品只能秒杀一次");
}
```

同一用户重复提交时，第二次会被直接拦截。

## 七、消息最终一致性

发送 Kafka 消息：

```java
kafkaTemplate.send(topicName, message.getRequestId(), toJson(message));
updateTransactionLog(message.getRequestId(), "QUEUED", true, false, null, null);
```

消费 Kafka 消息：

```java
@KafkaListener(topics = "${app.kafka.seckill-topic:seckill-orders}")
public void consume(String payload) {
    updateTransactionLog(message.getRequestId(), "PROCESSING", true, false, null, null);
    OrderEntity order = orderService.createOrder(message.getUsername(), request);
    updateTransactionLog(message.getRequestId(), "SUCCESS", true, true, order.getOrderNo(), null);
}
```

## 八、失败补偿

如果消费者创建订单失败：

```java
redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + message.getProductId(),
                                      message.getQuantity());

User user = userMapper.findByUsername(message.getUsername());
if (user != null) {
    redisTemplate.delete(USER_LOCK_KEY_PREFIX + user.getId() + ":" + message.getProductId());
}

updateTransactionLog(message.getRequestId(), "COMPENSATED", false, false, null, e.getMessage());
```

这样可以避免 Redis 库存被错误扣减，也允许用户后续重新参与。

## 九、下单和库存扣减一致

订单创建使用本地事务：

```java
@Transactional
public OrderEntity createOrder(String username, OrderCreateRequest request) {
    int updatedRows = inventoryMapper.deductStock(product.getId(), request.getQuantity());
    if (updatedRows <= 0) {
        throw new IllegalArgumentException("库存扣减失败");
    }

    orderMapper.insert(order);
    orderItemMapper.insert(item);
    return order;
}
```

库存 SQL：

```java
@Update("UPDATE inventory SET stock = stock - #{quantity} " +
        "WHERE product_id = #{productId} AND stock >= #{quantity}")
int deductStock(@Param("productId") Long productId,
                @Param("quantity") Integer quantity);
```

说明：

- 库存扣减成功，订单才会落库。
- 订单落库失败，事务回滚，库存扣减也回滚。
- SQL 条件更新保证库存不会扣成负数。

## 十、订单支付和订单状态一致

支付时会检查订单状态和过期时间，只有待支付订单可以支付：

```java
if (!"CREATED".equals(order.getStatus())) {
    throw new IllegalArgumentException("订单已支付、已取消或已超过支付时限");
}
```

支付成功后更新订单状态：

```java
orderMapper.markPaid(order.getId(), LocalDateTime.now());
```

支付逻辑避免重复支付：已支付、已取消、已超时订单不能再次支付。

## 十一、可验证接口

```powershell
$login = Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"student","password":"123456"}'

Invoke-RestMethod -Uri http://localhost/api/seckill/order `
  -Method POST `
  -ContentType 'application/json' `
  -Headers @{Authorization="Bearer $($login.token)"} `
  -Body '{"productId":4,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学测试地址"}'

Invoke-RestMethod -Uri http://localhost/api/system/transactions
```

## 十二、结论

本作业已完成基于消息队列的分布式事务方案。系统通过 Redis 预扣库存承接高并发请求，通过 Kafka 异步创建订单，通过 MySQL 本地事务保证订单和库存扣减一致，通过事务日志和失败补偿实现最终一致性。
