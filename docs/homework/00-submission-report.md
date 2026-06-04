# 商品库存与秒杀系统作业总报告

## 一、项目概述

本项目实现了一个商品库存与秒杀商城系统，包含用户端、管理员端、商品管理、库存管理、购物车、订单支付、优惠券、评论、物流模拟、消息通知、秒杀下单、高并发缓存、服务治理等功能。

系统重点围绕课程要求实现以下能力：

- 商品、库存、订单、用户基础业务建模。
- Docker Compose 一键启动 MySQL、Redis、Kafka、Nacos、Nginx、后端双实例。
- Nginx 实现静态资源托管、接口反向代理和负载均衡。
- Redis 实现商品详情缓存和秒杀库存预扣。
- Kafka 实现秒杀订单异步创建，削峰填谷。
- MySQL 条件更新实现库存防超卖。
- 事务日志和补偿逻辑实现最终一致性。
- Nacos 实现服务注册、配置发布和配置拉取。
- 运行时限流、熔断、降级实现服务治理。

## 二、作业完成情况总表

| 作业 | 要求 | 完成情况 |
| --- | --- | --- |
| 商品库存与秒杀系统设计 | 架构、API、ER、技术栈、登录注册 | 已完成 |
| 高并发读 | Docker、Nginx、动静分离、Redis 缓存、JMeter | 已完成 |
| 高并发读增强 | Redis 缓存、读写分离、Elasticsearch 可选 | Redis 已完成，读写分离未完成，ES 未接入 |
| 高并发写 | Redis 库存、Kafka 下单、雪花 ID、幂等、防超卖 | 已完成 |
| 分布式事务 | Redis 预扣、消息一致性、补偿、支付状态一致 | 已完成 |
| 服务治理 | Nacos、网关路由、动态配置、限流、熔断、压测 | 部分完成，Gateway 使用 Nginx 替代 |

## 三、项目目录说明

```text
backend/                 后端 Spring Boot 项目
frontend/                前端静态页面
nginx/nginx.conf         Nginx 网关、负载均衡、动静分离配置
docker-compose.yml       容器编排
Dockerfile               后端镜像构建文件
jmeter/                  JMeter 压测脚本
docs/homework/           作业提交文档
```

## 四、核心技术实现

### 1. 服务拆分

后端通过不同 Controller 和 Service 实现逻辑服务拆分：

```text
UserController       用户服务
ProductController    商品服务
InventoryController  库存服务
OrderController      订单服务
SeckillController    秒杀服务
GovernanceController 服务治理
```

### 2. 高并发读

商品详情采用 Redis 缓存：

```java
String cacheVal = redisTemplate.opsForValue().get(cacheKey);
if (cacheVal != null && !cacheVal.isBlank()) {
    return toProduct(cacheVal);
}
```

缓存异常场景处理：

```text
缓存穿透：空值缓存 NULL_PLACEHOLDER
缓存击穿：Redis setIfAbsent 分布式锁
缓存雪崩：缓存 TTL 加随机偏移
```

### 3. 高并发写

秒杀入口先扣 Redis 库存：

```java
Long stockAfterDeduct = redisTemplate.opsForValue().decrement(stockKey, quantity);
```

再发送 Kafka 消息异步创建订单：

```java
kafkaTemplate.send(topicName, message.getRequestId(), toJson(message));
```

### 4. 数据一致性

数据库使用条件更新防止超卖：

```java
UPDATE inventory
SET stock = stock - #{quantity}
WHERE product_id = #{productId}
AND stock >= #{quantity}
```

订单创建使用事务：

```java
@Transactional
public OrderEntity createOrder(String username, OrderCreateRequest request)
```

### 5. 服务治理

运行时限流：

```java
if (!bucket.tryAcquire()) {
    writeGovernanceResponse(response, 429, "当前访问过于频繁，已触发限流", "RATE_LIMITED");
}
```

熔断：

```java
if (failures >= threshold) {
    circuitOpenUntil = Instant.now().plusSeconds(openSeconds).toEpochMilli();
}
```

Nacos 配置同步：

```java
governanceConfigService.update(parts[0].trim(), parts[1].trim());
```

## 五、未完成和替代说明

1. MySQL 读写分离未完成  
当前项目只配置了一个 MySQL 容器，没有主从复制和动态数据源。

2. Elasticsearch 未接入  
题目中 Elasticsearch 为可选项，当前搜索使用 MySQL LIKE 实现。

3. ShardingSphere 分库分表未接入  
题目中分库分表为选做项，当前订单使用单库单表。

4. Spring Cloud Gateway 未接入  
当前使用 Nginx 实现统一入口、反向代理和双实例负载均衡。

## 六、提交文档

每个作业的单独文档如下：

```text
docs/homework/01-system-design.md
docs/homework/02-high-concurrency-read.md
docs/homework/03-high-concurrency-write.md
docs/homework/04-distributed-transaction.md
docs/homework/05-service-governance.md
```

## 七、结论

本项目完成了商品库存与秒杀系统的主要课程要求，重点实现了高并发读缓存、高并发写削峰、库存防超卖、秒杀最终一致性和服务治理。对于读写分离、Elasticsearch、ShardingSphere 和 Spring Cloud Gateway，项目进行了明确说明，作为后续扩展方向。
