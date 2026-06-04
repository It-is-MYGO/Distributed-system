# 分布式秒杀系统设计文档

## 一、设计目标

本系统设计目标是实现一个具备完整商城业务闭环和高并发秒杀能力的分布式系统演示项目。系统既要满足普通电商购物流程，也要体现商品库存、秒杀、缓存、消息队列、服务治理等分布式系统知识点。

## 二、总体架构

```text
浏览器
  |
  v
Nginx
  |-- 静态资源：frontend/
  |
  |-- /api/ -> app1:8081
  |-- /api/ -> app2:8082
               |
               |-- Spring Security + JWT
               |-- User Module
               |-- Product Module
               |-- Inventory Module
               |-- Cart Module
               |-- Order Module
               |-- Seckill Module
               |-- Governance Module
               |
               |-- MySQL
               |-- Redis
               |-- Kafka
               |-- Nacos
```

系统当前采用模块化单体实现，按照领域拆分 Controller、Service、Mapper。这样可以快速完成课程演示，同时保留后续拆分微服务的边界。

## 三、部署架构

Docker Compose 启动以下容器：

```text
ds-mysql       MySQL 数据库
ds-redis       Redis 缓存
ds-zookeeper   Kafka 依赖
ds-kafka       Kafka 消息队列
ds-nacos       Nacos 注册与配置中心
ds-app-1       后端实例 1，端口 8081
ds-app-2       后端实例 2，端口 8082
ds-nginx       Nginx 网关，宿主机端口 80
```

Nginx 负载均衡配置：

```nginx
upstream backend_round_robin {
    server app1:8081;
    server app2:8082;
}

location /api/ {
    proxy_pass http://backend_round_robin;
}
```

## 四、模块设计

### 1. 用户模块

职责：

- 注册账号。
- 登录并签发 JWT。
- 获取和修改个人信息。
- 上传头像。
- 模拟绑定邮箱/手机号。
- 模拟验证码修改密码。

主要接口：

```text
POST /api/user/register
POST /api/user/login
GET  /api/user/me
PUT  /api/user/me
```

### 2. 商品模块

职责：

- 商品列表。
- 商品搜索。
- 商品详情。
- 商品图片和详情长图。
- 商品评论。
- 秒杀价格和倒计时展示。

主要接口：

```text
GET /api/product
GET /api/product/search
GET /api/product/{id}
GET /api/product/{id}/images
GET /api/product/{id}/reviews
```

商品详情读取流程：

```text
请求商品详情
  |
  v
查询 Redis 缓存
  |
  |-- 命中：返回商品
  |
  |-- 未命中：获取分布式锁
          |
          v
        查询 MySQL
          |
          v
        写入 Redis，设置随机 TTL
```

### 3. 库存模块

职责：

- 查询商品库存。
- 扣减商品库存。
- 管理员调整库存。
- 防止库存超卖。

库存扣减 SQL：

```java
@Update("UPDATE inventory SET stock = stock - #{quantity} " +
        "WHERE product_id = #{productId} AND stock >= #{quantity}")
int deductStock(Long productId, Integer quantity);
```

### 4. 购物车模块

职责：

- 加入购物车。
- 修改数量。
- 删除购物车商品。
- 结算选中商品。
- Redis 缓存购物车，提高增删改体验。

设计要求：

- 同一用户同一商品只能有一条购物车记录。
- 重复加入时增加数量。
- 删除后立即刷新购物车缓存。

数据库约束：

```sql
UNIQUE KEY uk_cart_user_product (user_id, product_id)
```

### 5. 订单模块

职责：

- 直接购买创建订单。
- 购物车结算创建订单。
- 15 分钟待支付倒计时。
- 支付状态更新。
- 订单分类。
- 物流状态模拟。
- 签收、评价、追评。

订单状态设计：

```text
CREATED     待支付
CANCELLED   已取消
PAID        已支付
SHIPPING    运送中
DELIVERED   待签收
COMPLETED   已完成，待评价
REVIEWED    已评价
```

订单超时规则：

```text
创建后 15 分钟未支付，订单状态变为 CANCELLED。
```

### 6. 秒杀模块

职责：

- 秒杀请求入口。
- Redis 秒杀库存预扣。
- 用户限购。
- Kafka 异步创建订单。
- 秒杀事务日志。
- 失败补偿。

秒杀流程：

```text
用户提交秒杀
  |
  v
Redis setIfAbsent 判断是否重复秒杀
  |
  v
Redis decrement 预扣库存
  |
  v
写入事务日志 PENDING
  |
  v
发送 Kafka 消息，状态 QUEUED
  |
  v
消费者创建订单，状态 PROCESSING
  |
  v
成功：SUCCESS
失败：COMPENSATED 并回补 Redis 库存
```

### 7. 优惠券模块

职责：

- 平台发券。
- 用户领券。
- 下单选择优惠券。
- 根据满减门槛、商品、品类计算优惠。

数据表：

```text
coupon
user_coupon
```

### 8. 消息模块

职责：

- 支付提醒。
- 物流提醒。
- 签收提醒。
- 评价提醒。
- 优惠券领取提醒。
- 活动通知。

前端顶部使用铃铛入口，悬停预览最新消息，点击进入消息邮箱。

### 9. 管理员模块

职责：

- 商品管理。
- 库存管理。
- 秒杀设置。
- 优惠券设置。
- 用户管理。
- 数据统计和通知发布。

权限规则：

```text
USER：可以购买、支付、购物车、评价。
ADMIN：可以管理后台资源，不能执行普通用户购买行为。
```

## 五、数据库设计

核心表：

```text
user_account              用户表
goods_category            分类表
product                   商品表
product_detail_image      商品图片表
product_review            商品评价表
inventory                 库存表
shopping_cart_item        购物车表
orders                    订单表
order_item                订单明细表
coupon                    优惠券表
user_coupon               用户优惠券表
seckill_transaction_log   秒杀事务日志表
governance_config         服务治理配置表
```

订单与订单明细：

```text
orders 1 --- n order_item
```

商品与库存：

```text
product 1 --- 1 inventory
```

商品与评论：

```text
product 1 --- n product_review
```

## 六、缓存设计

### 商品详情缓存

Key：

```text
product:detail:{productId}
```

缓存策略：

- 命中：直接返回。
- 未命中：获取 Redis 锁后查询数据库。
- 不存在：写入空值缓存。
- TTL：基础时间 + 随机时间，避免雪崩。

### 秒杀库存缓存

Key：

```text
seckill:stock:{productId}
```

扣减方式：

```java
redisTemplate.opsForValue().decrement(stockKey, quantity);
```

### 秒杀用户限购锁

Key：

```text
seckill:once:{userId}:{productId}
```

写入方式：

```java
setIfAbsent(userLockKey, "1", Duration.ofMinutes(30))
```

## 七、消息队列设计

Topic：

```text
seckill-orders
```

消息内容：

```text
requestId
username
productId
quantity
receiverName
receiverPhone
address
```

生产者：

```java
kafkaTemplate.send(topicName, message.getRequestId(), toJson(message));
```

消费者：

```java
@KafkaListener(topics = "${app.kafka.seckill-topic:seckill-orders}")
public void consume(String payload)
```

## 八、一致性设计

### 普通订单

普通订单使用本地事务：

```java
@Transactional
public OrderEntity createOrder(...)
```

事务中完成：

```text
库存扣减
订单写入
订单明细写入
优惠券状态更新
```

### 秒杀订单

秒杀订单采用最终一致性：

```text
Redis 预扣库存 -> Kafka 消息 -> MySQL 本地事务 -> 事务日志 -> 失败补偿
```

## 九、服务治理设计

### Nacos

系统启动时：

- 注册服务实例。
- 发布默认治理配置。
- 定时发送心跳。
- 定时拉取配置。

### 限流

使用令牌桶算法：

```text
每秒补满 token。
每个请求消耗一个 token。
token 不足返回 429。
```

### 熔断

连续失败达到阈值后打开熔断：

```text
熔断打开期间返回 503。
熔断时间结束后恢复。
```

### 降级

对商品搜索等接口配置降级保护，避免非核心功能影响主链路。

## 十、安全设计

- 使用 JWT 认证。
- 登录后请求携带 `Authorization: Bearer <token>`。
- 管理员接口限制 ADMIN 角色。
- 普通用户和管理员功能隔离。
- 用户只能访问自己的购物车、订单、地址和消息。

## 十一、可扩展设计

后续可以扩展：

- 拆分为真实微服务。
- 引入 Spring Cloud Gateway。
- 接入 MySQL 主从读写分离。
- 接入 Elasticsearch。
- 接入 ShardingSphere 分库分表。
- 引入真实支付网关。
- 引入真实短信和邮件服务。

## 十二、设计结论

本系统通过模块化设计完成商城核心业务，通过 Redis、Kafka、Nginx、Nacos 等组件实现高并发读写和服务治理。系统重点解决了商品详情高频读取、秒杀库存超卖、重复下单、订单异步创建、流量过载等问题。
