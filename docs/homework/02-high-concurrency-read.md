# 作业二：高并发读

## 一、作业要求

1. 配置 Dockerfile 和 docker-compose，将数据库、后端服务、Nginx 分别用容器启动。
2. 后端启动多个实例，通过 Nginx 进行负载均衡。
3. 使用 JMeter 压力测试，观察响应时间和后端实例请求分布。
4. 实现动静分离，前端 HTML/CSS/JS 由 Nginx 托管。
5. 引入 Redis，实现商品详情页缓存。
6. 处理缓存穿透、缓存击穿、缓存雪崩。
7. 搭建 MySQL 读写分离环境并测试效果。
8. 可选：使用 Elasticsearch 实现商品搜索。

## 二、完成情况

已完成：Docker 容器化、Nginx 负载均衡、动静分离、Redis 商品详情缓存、缓存穿透/击穿/雪崩处理、JMeter 脚本。

未完成：MySQL 读写分离。

可选未接入：Elasticsearch。当前使用 MySQL LIKE 实现商品搜索。

## 三、Docker 容器环境

`docker-compose.yml` 中配置了 MySQL、Redis、Nacos、Kafka、Zookeeper、两个后端实例和 Nginx：

```yaml
services:
  mysql:
    image: mysql:8.4

  redis:
    image: redis:7.2

  app1:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      SERVER_PORT: 8081
      INSTANCE_ID: app-1

  app2:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      SERVER_PORT: 8082
      INSTANCE_ID: app-2

  nginx:
    image: nginx:1.27
    ports:
      - "80:80"
```

启动命令：

```powershell
docker compose up --build
```

## 四、Nginx 负载均衡

`nginx/nginx.conf` 中配置两个后端实例：

```nginx
upstream backend_round_robin {
    server app1:8081;
    server app2:8082;
}

location /api/ {
    proxy_pass http://backend_round_robin;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

同时配置了 least_conn 路由用于对比：

```nginx
upstream backend_least_conn {
    least_conn;
    server app1:8081;
    server app2:8082;
}
```

验证接口：

```powershell
1..8 | ForEach-Object {
  Invoke-RestMethod -Uri http://localhost/api/system/instance
}
```

多次访问可以观察请求在 `app-1` 和 `app-2` 之间切换。

## 五、动静分离

Nginx 直接托管前端静态资源：

```nginx
root /usr/share/nginx/html;
index index.html;

location / {
    try_files $uri $uri/ /index.html;
}
```

后端接口统一走 `/api/`：

```nginx
location /api/ {
    proxy_pass http://backend_round_robin;
}
```

这样静态页面请求不会进入 Spring Boot，后端只处理 API 请求。

## 六、Redis 商品详情缓存

商品详情查询位于 `ProductService#getDetail`。

先读 Redis：

```java
String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
String cacheVal = redisTemplate.opsForValue().get(cacheKey);

if (cacheVal != null && !cacheVal.isBlank()) {
    return toProduct(cacheVal);
}
```

缓存未命中时查询数据库：

```java
Product product = loadProductFromDatabase(id);
```

查询成功后写入 Redis：

```java
long ttlSeconds = 600 + ThreadLocalRandom.current().nextInt(0, 300);
redisTemplate.opsForValue().set(cacheKey, toJson(product), ttlSeconds, TimeUnit.SECONDS);
```

## 七、缓存穿透处理

当数据库也查不到商品时，写入短时间空值缓存：

```java
if (product == null) {
    redisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER, 2, TimeUnit.MINUTES);
    return null;
}
```

再次访问不存在的商品时直接返回，避免请求反复打到数据库。

## 八、缓存击穿处理

缓存未命中时使用 Redis 分布式锁：

```java
String lockKey = PRODUCT_LOCK_KEY_PREFIX + id;
locked = Boolean.TRUE.equals(
    redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 8, TimeUnit.SECONDS)
);
```

只有拿到锁的线程可以查询数据库并回写缓存，其他线程短暂等待后重试缓存。

## 九、缓存雪崩处理

缓存 TTL 加随机值，避免大量 Key 同一时间过期：

```java
long ttlSeconds = 600 + ThreadLocalRandom.current().nextInt(0, 300);
```

## 十、商品搜索

当前使用 MySQL LIKE 实现搜索：

```java
WHERE p.name LIKE CONCAT('%', #{keyword}, '%')
OR p.description LIKE CONCAT('%', #{keyword}, '%')
OR p.tag LIKE CONCAT('%', #{keyword}, '%')
OR c.name LIKE CONCAT('%', #{keyword}, '%')
```

说明：Elasticsearch 为可选项，当前未接入。

## 十一、读写分离说明

当前项目未接入 MySQL 主从读写分离，`docker-compose.yml` 中只有一个 MySQL 实例，后端也未配置动态数据源。

后续可扩展方案：

```text
MySQL Master：负责写入订单、库存扣减、用户注册
MySQL Slave：负责商品列表、商品详情、订单查询
Spring Dynamic Datasource：根据注解或方法名路由读写数据源
```

## 十二、JMeter 压测

项目提供了两个压测脚本：

```text
jmeter/api-load-test.jmx
jmeter/static-load-test.jmx
```

测试目标：

- `/api/product`：后端接口压力测试。
- `/shop.html`：静态资源压力测试。
- `/api/system/instance`：观察负载均衡效果。

## 十三、结论

本作业中的容器化、负载均衡、动静分离、Redis 分布式缓存和缓存异常场景处理已经完成。读写分离未实现，Elasticsearch 作为可选项未接入。
