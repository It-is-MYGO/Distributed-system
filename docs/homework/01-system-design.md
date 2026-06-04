# 作业一：商品库存与秒杀系统设计

## 一、作业要求

1. 绘制系统架构草图，服务拆分为用户服务、商品服务、订单服务、库存服务。
2. 定义各服务 RESTful API 接口。
3. 设计数据库 ER 图，包括用户表、商品表、库存表、订单表。
4. 说明技术栈选型。
5. 初始化项目代码仓库，搭建 Spring Boot + MyBatis + MySQL 基础环境。
6. 实现简单的用户注册、登录功能。

## 二、完成情况

已完成。

系统按照商城和秒杀业务拆分为用户、商品、库存、订单、购物车、优惠券、评论、秒杀、治理等模块。后端采用 Spring Boot + MyBatis，数据库使用 MySQL，前端通过 Nginx 静态托管。

## 三、系统架构设计

系统整体架构如下：

```text
用户浏览器
   |
   v
Nginx 网关 / 静态资源服务器
   |
   +--> 前端静态页面 frontend/
   |
   +--> 后端应用 app1:8081
   +--> 后端应用 app2:8082
           |
           +--> UserController 用户模块
           +--> ProductController 商品模块
           +--> InventoryController 库存模块
           +--> OrderController 订单模块
           +--> SeckillController 秒杀模块
           |
           +--> MySQL 持久化数据
           +--> Redis 缓存与秒杀库存预扣
           +--> Kafka 秒杀订单异步创建
           +--> Nacos 服务注册与配置治理
```

## 四、RESTful API 设计

主要接口如下：

```text
POST /api/user/register          用户注册
POST /api/user/login             用户登录
GET  /api/user/me                获取当前用户信息

GET  /api/product                商品列表
GET  /api/product/search         商品搜索
GET  /api/product/{id}           商品详情
GET  /api/product/{id}/reviews   商品评价

GET  /api/inventory/{productId}  查询库存
POST /api/inventory/{productId}/deduct 扣减库存

POST /api/order/create           直接下单
POST /api/order/checkout-cart    购物车结算
POST /api/order/{orderNo}/pay    支付订单
GET  /api/order/mine             我的订单

POST /api/seckill/order          秒杀下单
GET  /api/seckill/status/{id}    秒杀状态查询
```

## 五、数据库 ER 设计

核心表位于 `backend/src/main/resources/schema.sql`。

用户表：

```sql
CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nick_name VARCHAR(100),
  avatar_url LONGTEXT,
  email VARCHAR(120),
  phone VARCHAR(30),
  role VARCHAR(20) NOT NULL DEFAULT 'USER'
);
```

商品表：

```sql
CREATE TABLE IF NOT EXISTS product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  category_id BIGINT,
  description TEXT,
  cover_image VARCHAR(255),
  sales_count INT NOT NULL DEFAULT 0,
  original_price DECIMAL(10,2),
  seckill_price DECIMAL(10,2),
  seckill_start_at TIMESTAMP NULL,
  seckill_end_at TIMESTAMP NULL
);
```

库存表：

```sql
CREATE TABLE IF NOT EXISTS inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL UNIQUE,
  stock INT NOT NULL,
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id)
);
```

订单表：

```sql
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  receiver_name VARCHAR(100),
  receiver_phone VARCHAR(30),
  user_address VARCHAR(255),
  paid_at TIMESTAMP NULL
);
```

订单明细表：

```sql
CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(120),
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL
);
```

## 六、技术栈选型

- Spring Boot：快速构建后端服务。
- Spring Security + JWT：实现登录认证和权限控制。
- MyBatis：操作 MySQL 数据库。
- MySQL：保存用户、商品、库存、订单等核心数据。
- Redis：商品详情缓存、购物车缓存、秒杀库存预扣。
- Kafka：秒杀订单异步创建，削峰填谷。
- Nginx：静态资源托管、反向代理、负载均衡。
- Docker Compose：统一启动 MySQL、Redis、Kafka、Nacos、Nginx 和后端服务。

## 七、关键代码

库存服务接口：

```java
@PostMapping("/{productId}/deduct")
public ResponseEntity<?> deduct(@PathVariable Long productId,
                                @RequestParam Integer quantity) {
    int updated = inventoryService.deductStock(productId, quantity);
    if (updated <= 0) {
        return ResponseEntity.badRequest().body("库存扣减失败");
    }
    return ResponseEntity.ok(updated);
}
```

库存原子扣减：

```java
@Update("UPDATE inventory SET stock = stock - #{quantity} " +
        "WHERE product_id = #{productId} AND stock >= #{quantity}")
int deductStock(@Param("productId") Long productId,
                @Param("quantity") Integer quantity);
```

用户注册登录使用 `UserController` 和 Spring Security/JWT 完成，登录成功后返回 Token，后续请求通过 `Authorization: Bearer token` 鉴权。

## 八、结论

本作业要求已完成。系统具备完整的商品、库存、订单、用户基础结构，并已经扩展到秒杀、优惠券、评价、物流和管理员管理功能。
