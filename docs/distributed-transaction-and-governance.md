# 分布式事务与服务治理验证说明

## 1. 分布式事务与一致性

秒杀下单链路：

1. 用户通过 `/api/seckill/order` 提交秒杀请求。
2. `SeckillOrderService` 先在 Redis 中预扣 `seckill:stock:{productId}`，并用 `seckill:once:{userId}:{productId}` 做同一用户同一商品幂等。
3. 写入 `seckill_transaction_log`，状态为 `PENDING`。
4. 消息投递 Kafka 成功后状态变为 `QUEUED`。
5. 消费端创建订单，`OrderService#createOrder` 在本地事务中完成订单落库、订单明细落库、MySQL 库存原子扣减。
6. 成功后事务日志变为 `SUCCESS`；失败则回补 Redis 库存、删除用户幂等锁，状态变为 `COMPENSATED`。

可验证接口：

```powershell
$login = Invoke-RestMethod -Uri http://localhost/api/user/login -Method POST -ContentType 'application/json' -Body '{"username":"student","password":"123456"}'
Invoke-RestMethod -Uri http://localhost/api/seckill/order -Method POST -ContentType 'application/json' -Headers @{Authorization="Bearer $($login.token)"} -Body '{"productId":4,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学测试地址"}'
Invoke-RestMethod -Uri http://localhost/api/system/transactions
```

重点观察：

- `PENDING/QUEUED/PROCESSING/SUCCESS` 表示最终一致。
- `COMPENSATED` 表示消费失败后已补偿 Redis 库存和用户锁。
- 普通订单的“下单 + 库存扣减”由 `@Transactional` 保证本地一致性。
- “订单支付 + 订单状态更新”由 `OrderService#pay` 通过状态条件更新避免重复支付。

## 2. 服务注册发现与配置治理

Docker Compose 已加入 Nacos standalone：

- Nacos 控制台：`http://localhost:8849/nacos`（容器内服务地址仍为 `nacos:8848`）
- 网关地址：`http://localhost/api/**`
- 两个后端实例：`app1:8081`、`app2:8082`

项目保留 Nginx 作为可运行网关入口，同时提供服务治理视图：

```powershell
$admin = Invoke-RestMethod -Uri http://localhost/api/user/login -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}'
Invoke-RestMethod -Uri http://localhost/api/governance/service-registry -Headers @{Authorization="Bearer $($admin.token)"}
```

Nacos 配置发布与拉取：

```powershell
$h = @{Authorization="Bearer $($admin.token)"}
Invoke-RestMethod -Uri http://localhost/api/governance/nacos/publish -Method POST -Headers $h
Invoke-RestMethod -Uri http://localhost/api/governance/nacos/config -Headers $h
```

当通过 `/api/governance/config` 修改 `traffic.rate-limit.permits-per-second` 等属性时，后端会同步发布到 Nacos 的 `mall-governance.properties`，并立即影响运行时治理逻辑。

动态路由验证：

```powershell
1..8 | ForEach-Object { Invoke-RestMethod -Uri http://localhost/api/system/instance }
```

多次调用可观察 `instanceId` 在 `app-1/app-2` 之间变化，验证网关动态路由/负载均衡。

## 3. 动态配置、限流、熔断和降级

治理配置保存在 `governance_config`，运行时可改，不需要重启应用：

```powershell
$admin = Invoke-RestMethod -Uri http://localhost/api/user/login -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}'
$h = @{Authorization="Bearer $($admin.token)"}
Invoke-RestMethod -Uri http://localhost/api/governance/config -Headers $h
Invoke-RestMethod -Uri http://localhost/api/governance/config -Method POST -ContentType 'application/json' -Headers $h -Body '{"key":"traffic.rate-limit.permits-per-second","value":"5"}'
Invoke-RestMethod -Uri http://localhost/api/governance/traffic/status -Headers $h
```

压测限流效果：

```powershell
1..30 | ForEach-Object {
  try { Invoke-WebRequest -UseBasicParsing -Uri http://localhost/api/product | Select-Object -ExpandProperty StatusCode }
  catch { $_.Exception.Response.StatusCode.value__ }
}
```

当超过阈值时会返回 `429 RATE_LIMITED`。后端连续 5xx 达到阈值后会打开熔断，返回 `503 CIRCUIT_OPEN`。

## 4. JMeter 压测

已有脚本：

- `jmeter/api-load-test.jmx`
- `jmeter/static-load-test.jmx`

建议测试：

- 静态页：`/shop.html`
- 商品接口：`/api/product`
- 秒杀接口：`/api/seckill/order`
- 实例探针：`/api/system/instance`

对比指标：

- Nginx 日志中 `app-1/app-2` 请求分布是否接近。
- 开启低限流阈值后 429 比例是否上升。
- 秒杀高并发下 `seckill_transaction_log` 是否最终落到 `SUCCESS` 或 `COMPENSATED`，库存不超卖。
