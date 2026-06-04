# 附录：启动与验证手册

## 一、启动项目

在项目根目录执行：

```powershell
docker compose up --build
```

启动完成后访问：

```text
前端首页：http://localhost
Nacos 控制台：http://localhost:8849/nacos
MySQL 端口：localhost:3307
```

## 二、测试账号

```text
普通用户：
用户名：student
密码：123456

管理员：
用户名：admin
密码：admin123
```

说明：前端登录页不直接展示测试账号，账号仅用于测试和验收。

## 三、验证负载均衡

请求实例探针：

```powershell
1..10 | ForEach-Object {
  Invoke-RestMethod -Uri http://localhost/api/system/instance
}
```

预期结果：

```text
返回结果中的 instanceId 会在 app-1 和 app-2 之间变化。
```

## 四、验证商品详情缓存

访问商品详情：

```powershell
Invoke-RestMethod -Uri http://localhost/api/product/1
```

预期结果：

```text
第一次访问查数据库并写入 Redis。
后续访问优先从 Redis 返回商品详情。
不存在的商品会写入短时间空值缓存，防止缓存穿透。
```

## 五、验证商品搜索

```powershell
Invoke-RestMethod -Uri "http://localhost/api/product/search?keyword=手机"
```

说明：

```text
当前搜索使用 MySQL LIKE。
Elasticsearch 未接入，属于可选项。
```

## 六、验证普通下单

登录：

```powershell
$login = Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"student","password":"123456"}'
```

创建订单：

```powershell
Invoke-RestMethod -Uri http://localhost/api/order/create `
  -Method POST `
  -ContentType 'application/json' `
  -Headers @{Authorization="Bearer $($login.token)"} `
  -Body '{"productId":1,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学"}'
```

预期结果：

```text
订单创建成功，库存同步扣减。
如果库存不足，订单创建失败。
```

## 七、验证秒杀下单

```powershell
Invoke-RestMethod -Uri http://localhost/api/seckill/order `
  -Method POST `
  -ContentType 'application/json' `
  -Headers @{Authorization="Bearer $($login.token)"} `
  -Body '{"productId":4,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学"}'
```

预期结果：

```text
接口返回 QUEUED，表示秒杀请求已进入 Kafka 队列。
后端消费者异步创建订单。
```

查看事务状态：

```powershell
Invoke-RestMethod -Uri http://localhost/api/system/transactions
```

状态说明：

```text
PENDING     Redis 预扣成功
QUEUED      Kafka 入队成功
PROCESSING  消费端处理中
SUCCESS     订单创建成功
COMPENSATED 订单创建失败，库存已补偿
```

## 八、验证限流

管理员登录：

```powershell
$admin = Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"admin","password":"admin123"}'

$h = @{Authorization="Bearer $($admin.token)"}
```

降低限流阈值：

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/config `
  -Method POST `
  -ContentType 'application/json' `
  -Headers $h `
  -Body '{"key":"traffic.rate-limit.permits-per-second","value":"5"}'
```

快速请求：

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

预期结果：

```text
超过阈值时返回 429。
```

## 九、验证熔断状态

查看治理状态：

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/traffic/status -Headers $h
```

预期字段：

```text
rateLimitEnabled
permitsPerSecond
circuitBreakerEnabled
failureCount
circuitOpen
circuitOpenUntil
```

## 十、验证 Nacos 配置

发布配置：

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/nacos/publish `
  -Method POST `
  -Headers $h
```

拉取配置：

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/nacos/config `
  -Headers $h
```

## 十一、JMeter 压测脚本

脚本位置：

```text
jmeter/api-load-test.jmx
jmeter/static-load-test.jmx
```

测试建议：

```text
静态资源压测：http://localhost/shop.html
商品接口压测：http://localhost/api/product
秒杀接口压测：http://localhost/api/seckill/order
实例探针压测：http://localhost/api/system/instance
```

## 十二、常见说明

如果 `localhost` 打不开，需要确认 Nginx 容器是否启动：

```powershell
docker compose ps
```

如果后端接口失败，查看日志：

```powershell
docker compose logs app1
docker compose logs app2
```

如果 Nacos 打不开，访问：

```text
http://localhost:8849/nacos
```

不是 `8848`，因为宿主机映射端口为 `8849`。
