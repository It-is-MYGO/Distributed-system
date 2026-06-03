# 高并发要求对照清单

## 已实现

| 要求 | 实现位置 |
| --- | --- |
| Docker 容器环境 | `docker-compose.yml` 编排 MySQL、Redis、Kafka、Zookeeper、Nginx、app1、app2 |
| 后端多实例 | `docker-compose.yml` 中 `app1` 端口 8081、`app2` 端口 8082 |
| Nginx 负载均衡 | `nginx/nginx.conf` 的 `backend_round_robin` 和 `backend_least_conn` |
| 静态/动态分离 | Nginx `root /usr/share/nginx/html` 托管前端，`/api/` 代理后端 |
| 商品详情 Redis 缓存 | `ProductService#getDetail` |
| 缓存穿透 | 商品不存在时写入 `NULL` 空值缓存 |
| 缓存击穿 | `lock:product:{id}` 互斥锁保护热点商品回源 |
| 缓存雪崩 | 商品缓存 TTL 使用 600-900 秒随机过期 |
| 库存服务 | `InventoryController`、`InventoryService`、`InventoryMapper`、`inventory` 表 |
| 防超卖 | `InventoryMapper#deductStock` 使用 `stock >= quantity` 原子扣减 |
| Kafka 秒杀异步下单 | `SeckillOrderService#submit` 发送消息，`consume` 消费创建订单 |
| Redis 秒杀库存预扣 | `seckill:stock:{productId}` |
| 秒杀幂等 | `seckill:once:{userId}:{productId}` 限制同一用户同一商品一次 |
| 雪花式 ID | `SnowflakeIdService`，订单号与秒杀请求号使用该 ID |
| 订单查询 | `/api/order/mine`、`/api/order/{orderId}` |
| 能力核验接口 | `/api/system/capabilities` |

## 部分实现或选做未接入

| 要求 | 当前状态 |
| --- | --- |
| JMeter 压测 | 项目已有可压测接口和双实例日志，但未提交 JMeter 脚本 |
| MySQL 主从读写分离 | 当前 Docker 只有单 MySQL；能力清单接口已标明。真正实现需增加 master/slave 容器并接入动态数据源 |
| Elasticsearch 搜索 | 作业标注“可选”，当前使用 MySQL LIKE 搜索 |
| ShardingSphere 分库分表 | 作业标注“选做”，当前未接入 ShardingSphere |

## 关键验证命令

```powershell
Invoke-WebRequest -UseBasicParsing -Uri http://localhost/api/system/capabilities
```

```powershell
$login = Invoke-WebRequest -UseBasicParsing -Uri http://localhost/api/user/login -Method POST -ContentType 'application/json' -Body '{"username":"student","password":"123456"}' | ConvertFrom-Json
Invoke-WebRequest -UseBasicParsing -Uri http://localhost/api/seckill/order -Method POST -ContentType 'application/json' -Headers @{Authorization="Bearer $($login.token)"} -Body '{"productId":4,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学测试地址"}'
```
