# 作业三：高并发写

## 一、作业要求

1. 实现秒杀下单功能。
2. 使用 Redis 缓存库存。
3. 使用 Kafka 异步处理订单创建，削峰填谷。
4. 使用雪花算法或基因算法生成订单 ID。
5. 保证幂等性：同一用户同一商品只能秒杀一次。
6. 保证数据一致性：最终库存不超卖，订单数据完整。
7. 选做：使用 ShardingSphere 实现订单表分库分表。

## 二、完成情况

已完成：秒杀下单、Redis 预扣库存、Kafka 异步创建订单、雪花算法订单号、同一用户同一商品限购、数据库库存原子扣减、失败补偿、秒杀事务日志。

未完成：ShardingSphere 分库分表。该项为选做。

## 三、秒杀下单接口

秒杀入口为：

```text
POST /api/seckill/order
```

用户提交秒杀请求后，后端不会直接同步创建订单，而是先预扣 Redis 库存，再发送 Kafka 消息。

## 四、Redis 秒杀库存预扣

核心代码位于 `SeckillOrderService#submit`：

```java
String stockKey = STOCK_KEY_PREFIX + request.getProductId();
initStockCacheIfAbsent(request.getProductId(), stockKey);

Long stockAfterDeduct = redisTemplate.opsForValue().decrement(stockKey, quantity);
if (stockAfterDeduct == null || stockAfterDeduct < 0) {
    redisTemplate.opsForValue().increment(stockKey, quantity);
    redisTemplate.delete(userLockKey);
    throw new IllegalArgumentException("秒杀库存不足");
}
```

说明：

- Redis `decrement` 是原子操作。
- 当扣减后小于 0 时，说明库存不足，立即回滚 Redis 库存。
- 请求不会进入数据库，减少数据库压力。

## 五、秒杀库存初始化

Redis 中没有库存 Key 时，从 MySQL 库存表初始化：

```java
private void initStockCacheIfAbsent(Long productId, String stockKey) {
    if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
        return;
    }
    Inventory inventory = inventoryMapper.findByProductId(productId);
    if (inventory == null) {
        throw new IllegalArgumentException("商品库存不存在");
    }
    redisTemplate.opsForValue()
        .setIfAbsent(stockKey, String.valueOf(inventory.getStock()), Duration.ofMinutes(30));
}
```

## 六、幂等性与限购

同一用户同一商品只能秒杀一次：

```java
String userLockKey = USER_LOCK_KEY_PREFIX + user.getId() + ":" + request.getProductId();
Boolean firstSubmit = redisTemplate.opsForValue()
    .setIfAbsent(userLockKey, "1", Duration.ofMinutes(30));

if (!Boolean.TRUE.equals(firstSubmit)) {
    throw new IllegalArgumentException("同一用户同一商品只能秒杀一次");
}
```

这里使用 Redis `setIfAbsent` 实现幂等锁。

## 七、Kafka 异步创建订单

秒杀请求预扣库存成功后，发送 Kafka 消息：

```java
kafkaTemplate.send(topicName, message.getRequestId(), toJson(message));
updateTransactionLog(message.getRequestId(), "QUEUED", true, false, null, null);
```

Kafka 消费端异步创建订单：

```java
@KafkaListener(topics = "${app.kafka.seckill-topic:seckill-orders}",
               groupId = "${spring.kafka.consumer.group-id:ds-seckill-group}")
public void consume(String payload) {
    OrderEntity order = orderService.createOrder(message.getUsername(), request);
    updateTransactionLog(message.getRequestId(), "SUCCESS", true, true, order.getOrderNo(), null);
}
```

这种方式可以把高并发瞬时请求削峰，订单创建由消费者逐步处理。

## 八、订单号生成

订单号使用雪花算法生成，代码位于 `SnowflakeIdService`：

```java
public synchronized long nextId() {
    long timestamp = System.currentTimeMillis();
    if (timestamp == lastTimestamp) {
        sequence = (sequence + 1) & SEQUENCE_MASK;
        if (sequence == 0) {
            timestamp = waitNextMillis(lastTimestamp);
        }
    } else {
        sequence = 0L;
    }
    lastTimestamp = timestamp;
    return ((timestamp - EPOCH) << (WORKER_ID_BITS + SEQUENCE_BITS))
            | (workerId << SEQUENCE_BITS)
            | sequence;
}
```

订单号生成：

```java
order.setOrderNo("ORD" + snowflakeIdService.nextId());
```

## 九、数据库库存防超卖

MySQL 层使用条件更新保证库存不为负：

```java
@Update("UPDATE inventory SET stock = stock - #{quantity} " +
        "WHERE product_id = #{productId} AND stock >= #{quantity}")
int deductStock(@Param("productId") Long productId,
                @Param("quantity") Integer quantity);
```

如果并发请求同时扣库存，只有满足 `stock >= quantity` 的请求会成功。

## 十、订单本地事务

订单创建和库存扣减放在同一个本地事务中：

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

如果订单写入失败，事务回滚，数据库库存扣减也会回滚。

## 十一、失败补偿

Kafka 消费失败时，系统会回补 Redis 库存并释放用户秒杀锁：

```java
redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + message.getProductId(),
                                      message.getQuantity());
redisTemplate.delete(USER_LOCK_KEY_PREFIX + user.getId() + ":" + message.getProductId());
updateTransactionLog(message.getRequestId(), "COMPENSATED", false, false, null, e.getMessage());
```

## 十二、事务日志

秒杀事务日志表：

```sql
CREATE TABLE IF NOT EXISTS seckill_transaction_log (
  request_id VARCHAR(80) NOT NULL UNIQUE,
  username VARCHAR(100) NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  redis_deducted TINYINT NOT NULL DEFAULT 0,
  db_deducted TINYINT NOT NULL DEFAULT 0,
  order_no VARCHAR(64),
  error_message VARCHAR(500)
);
```

状态流转：

```text
PENDING -> QUEUED -> PROCESSING -> SUCCESS
失败时：COMPENSATED
```

## 十三、分库分表说明

ShardingSphere 分库分表未实现。当前订单表为单库单表：

```text
orders
order_item
```

由于题目中分库分表标注为选做，因此本项目将其列为后续扩展。

## 十四、结论

本作业核心要求已完成。系统使用 Redis 承接秒杀库存压力，Kafka 异步创建订单，MySQL 条件更新防止超卖，并通过事务日志和失败补偿保证最终一致性。
