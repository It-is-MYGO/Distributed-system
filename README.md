# 分布式秒杀系统

本项目是一个面向课程作业和演示验收的商品库存与秒杀商城系统，覆盖商品展示、搜索、购物车、下单、支付模拟、订单物流、评价、优惠券、用户消息、管理员管理、Redis 缓存、Kafka 秒杀削峰、Nginx 负载均衡、Nacos 服务治理等功能。

## 一、项目目标

系统围绕“商品库存与秒杀系统设计”实现以下目标：

- 构建完整商城前后端功能。
- 实现用户、商品、库存、订单、秒杀等核心模块。
- 支持高并发读：Redis 商品详情缓存、缓存穿透/击穿/雪崩处理。
- 支持高并发写：Redis 预扣库存、Kafka 异步下单、库存防超卖。
- 支持容器化部署：MySQL、Redis、Kafka、Nacos、Nginx、后端双实例。
- 支持服务治理：Nacos 配置、限流、熔断、降级。
- 支持压测验证：JMeter 静态资源和后端接口压测脚本。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | HTML、CSS、JavaScript |
| 后端 | Spring Boot、Spring Security、JWT、MyBatis |
| 数据库 | MySQL |
| 缓存 | Redis |
| 消息队列 | Kafka、Zookeeper |
| 服务治理 | Nacos、运行时治理配置、限流、熔断、降级 |
| 网关与负载均衡 | Nginx |
| 容器化 | Docker、Docker Compose |
| 压测 | JMeter |

## 三、目录结构

```text
backend/                 后端 Spring Boot 项目
frontend/                前端静态页面
nginx/nginx.conf         Nginx 静态托管、反向代理、负载均衡配置
jmeter/                  JMeter 压测脚本
docs/                    项目文档
docs/homework/           按作业拆分的提交文档
docker-compose.yml       Docker Compose 编排文件
Dockerfile               后端镜像构建文件
README.md                项目说明
```

## 四、核心功能

### 用户端

- 用户注册、登录、退出、切换账号。
- 个人中心、头像上传、绑定邮箱/手机号模拟、修改密码验证码模拟。
- 商品首页、分类、搜索、商品详情、图片轮播、商品评价。
- 加入购物车、购物车数量修改、删除、结算。
- 收货地址新增、删除、展开选择。
- 优惠券领取、选择、满减计算。
- 订单提交、15 分钟待支付倒计时、支付方式选择。
- 银行卡、微信、支付宝、先付后享等模拟支付。
- 订单分类：全部、运送中、待签收、已完成、已评价、未完成。
- 物流模拟、签收、评价、追评。
- 消息邮箱：订单、物流、评价、优惠券、活动提醒。

### 管理员端

- 商品管理：新增、编辑、价格、库存、秒杀时间设置。
- 库存管理：查看和调整商品库存。
- 销量统计和商品分析。
- 用户信息管理。
- 优惠券和优惠规则管理。
- 活动和全平台消息通知。
- 管理员鉴权：管理员不提供普通用户购买、支付、加入购物车等功能。

### 高并发能力

- Redis 商品详情缓存。
- 缓存穿透：空值缓存。
- 缓存击穿：Redis 分布式锁。
- 缓存雪崩：随机 TTL。
- Redis 秒杀库存预扣。
- 同一用户同一商品秒杀幂等限制。
- Kafka 秒杀订单异步创建。
- MySQL 条件更新防止库存超卖。
- 秒杀事务日志与失败补偿。

## 五、快速启动

在项目根目录执行：

```powershell
docker compose up --build
```

启动后访问：

```text
前端首页：http://localhost
Nacos 控制台：http://localhost:8849/nacos
MySQL：localhost:3307
```

查看容器状态：

```powershell
docker compose ps
```

查看后端日志：

```powershell
docker compose logs app1
docker compose logs app2
```

## 六、测试账号

```text
普通用户：
用户名：student
密码：123456

管理员：
用户名：admin
密码：admin123
```

## 七、关键接口

```text
POST /api/user/register          用户注册
POST /api/user/login             用户登录
GET  /api/user/me                当前用户

GET  /api/product                商品列表
GET  /api/product/search         商品搜索
GET  /api/product/{id}           商品详情

GET  /api/inventory/{productId}  查询库存
POST /api/inventory/{productId}/deduct 扣减库存

POST /api/cart/add               加入购物车
GET  /api/cart                   购物车列表

POST /api/order/create           直接下单
POST /api/order/checkout-cart    购物车结算
POST /api/order/{orderNo}/pay    支付订单
GET  /api/order/mine             我的订单

POST /api/seckill/order          秒杀下单
GET  /api/seckill/status/{id}    秒杀状态查询

GET  /api/governance/config      治理配置
POST /api/governance/config      修改治理配置
GET  /api/governance/traffic/status 流量治理状态
```

## 八、验证命令

### 负载均衡

```powershell
1..10 | ForEach-Object {
  Invoke-RestMethod -Uri http://localhost/api/system/instance
}
```

预期：返回结果中的实例会在 `app-1` 和 `app-2` 之间变化。

### 秒杀下单

```powershell
$login = Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"student","password":"123456"}'

Invoke-RestMethod -Uri http://localhost/api/seckill/order `
  -Method POST `
  -ContentType 'application/json' `
  -Headers @{Authorization="Bearer $($login.token)"} `
  -Body '{"productId":4,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学"}'
```

### 限流验证

```powershell
1..30 | ForEach-Object {
  try {
    Invoke-WebRequest -UseBasicParsing -Uri http://localhost/api/product |
      Select-Object -ExpandProperty StatusCode
  } catch {
    $_.Exception.Response.StatusCode.value__
  }
}
```

高并发下可能返回 `429 RATE_LIMITED`。

## 九、项目文档

正式文档：

- [需求规约文档](./docs/requirements-specification.md)
- [系统设计文档](./docs/system-design-document.md)
- [测试说明文档](./docs/test-instructions.md)

作业提交文档：

- [作业总报告](./docs/homework/00-submission-report.md)
- [作业一：商品库存与秒杀系统设计](./docs/homework/01-system-design.md)
- [作业二：高并发读](./docs/homework/02-high-concurrency-read.md)
- [作业三：高并发写](./docs/homework/03-high-concurrency-write.md)
- [作业四：分布式事务](./docs/homework/04-distributed-transaction.md)
- [作业五：服务治理](./docs/homework/05-service-governance.md)

## 十、未实现和替代说明

- MySQL 读写分离未接入，当前为单 MySQL 实例。
- Elasticsearch 未接入，当前搜索使用 MySQL LIKE；题目中该项为可选。
- ShardingSphere 分库分表未接入；题目中该项为选做。
- Spring Cloud Gateway 未接入，当前使用 Nginx 实现统一入口、反向代理和负载均衡。

## 十一、结论

本系统已经具备完整商城业务闭环，并实现了商品库存秒杀系统的关键高并发能力：Redis 缓存、Kafka 削峰、库存防超卖、事务补偿、限流熔断和容器化双实例部署，适合用于课程作业提交、演示和压测验证。
