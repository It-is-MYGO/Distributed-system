# 分布式秒杀系统测试说明文档

## 一、测试目的

本文档说明系统的启动方式、测试环境、测试账号、功能测试用例、接口测试命令、压测方法和预期结果，用于项目验收和问题排查。

## 二、测试环境

| 项目 | 说明 |
| --- | --- |
| 操作系统 | Windows |
| 容器 | Docker Desktop |
| 数据库 | MySQL 8.4 |
| 缓存 | Redis 7.2 |
| 消息队列 | Kafka 7.6.0 |
| 注册配置中心 | Nacos 2.3.2 |
| 网关 | Nginx 1.27 |
| 后端 | Spring Boot |
| 前端 | 静态 HTML/CSS/JS |

## 三、启动系统

在项目根目录执行：

```powershell
docker compose up --build
```

启动完成后访问：

```text
前端：http://localhost
Nacos：http://localhost:8849/nacos
MySQL：localhost:3307
```

查看容器：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs app1
docker compose logs app2
docker compose logs nginx
```

## 四、测试账号

```text
普通用户：
username: student
password: 123456

管理员：
username: admin
password: admin123
```

## 五、功能测试用例

### 1. 用户登录

步骤：

1. 打开 `http://localhost`。
2. 点击登录。
3. 输入普通用户账号。
4. 登录成功后进入首页。

预期结果：

```text
顶部显示当前用户头像和昵称。
用户可以访问购物车、订单、个人中心。
```

接口测试：

```powershell
Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"student","password":"123456"}'
```

### 2. 商品浏览和搜索

步骤：

1. 打开首页。
2. 查看商品推荐列表。
3. 点击分类。
4. 搜索关键词，例如“手机”。

预期结果：

```text
商品正常展示。
分类可以筛选商品。
搜索结果展示商品图片、名称、价格、销量、标签。
```

接口测试：

```powershell
Invoke-RestMethod -Uri http://localhost/api/product
Invoke-RestMethod -Uri "http://localhost/api/product/search?keyword=手机"
```

### 3. 商品详情

步骤：

1. 点击任意商品。
2. 进入商品详情页。
3. 查看轮播图、价格、商品详情、评论区。

预期结果：

```text
商品详情正常加载。
图片可以翻页。
评论区显示星级、头像、好评率。
未登录点击购买或加入购物车时弹出登录窗口。
```

接口测试：

```powershell
Invoke-RestMethod -Uri http://localhost/api/product/1
Invoke-RestMethod -Uri http://localhost/api/product/1/reviews
```

### 4. 加入购物车

步骤：

1. 登录普通用户。
2. 在首页、分类页或详情页点击加入购物车。
3. 多次点击同一商品。
4. 打开购物车。

预期结果：

```text
同一商品只出现一条购物车记录。
重复加入只增加数量。
购物车数量同步变化。
加入购物车有动画提示。
```

### 5. 购物车删除

步骤：

1. 打开购物车。
2. 删除某个商品。

预期结果：

```text
商品立即从购物车列表消失。
刷新页面后仍然保持删除结果。
```

### 6. 购物车结算

步骤：

1. 选择多个购物车商品。
2. 点击去结算。
3. 进入确认订单页。

预期结果：

```text
确认订单页展示购物车中选择的全部商品。
金额为所有选中商品合计，并正确应用优惠券。
提交订单后对应购物车商品被清除。
```

### 7. 收货地址

步骤：

1. 进入确认订单页。
2. 新增收货地址。
3. 选择已有收货地址。
4. 删除地址。

预期结果：

```text
地址只属于当前登录用户。
多个地址可以展开查看。
删除后当前用户不可再看到该地址。
其他账号不能看到该用户地址。
```

### 8. 提交订单和倒计时

步骤：

1. 提交订单。
2. 进入支付页。
3. 查看倒计时。
4. 返回订单列表。

预期结果：

```text
订单进入待支付状态。
支付页显示 15 分钟倒计时。
订单列表未完成分类也显示倒计时。
超过 15 分钟后订单自动取消。
```

### 9. 支付方式

步骤：

1. 在支付页选择银行卡、微信、支付宝或先付后享。
2. 点击立即支付。
3. 微信和支付宝进入二维码模拟支付页。
4. 点击模拟支付完成。

预期结果：

```text
支付成功后订单状态更新。
订单进入运送中或后续物流状态。
消息邮箱收到支付成功提醒。
```

### 10. 物流签收和评价

步骤：

1. 支付订单。
2. 等待物流状态更新到待签收。
3. 点击签收。
4. 在已完成订单中点击评价。
5. 在已评价订单中点击追评。

预期结果：

```text
签收后订单进入已完成。
评价后订单进入已评价。
追评显示在商品评论区对应评论下方。
```

### 11. 优惠券

步骤：

1. 进入优惠券弹窗。
2. 领取优惠券。
3. 下单时选择优惠券。

预期结果：

```text
满足门槛时优惠券可以抵扣金额。
不满足门槛或不适用商品/品类时不能使用。
订单总金额正确计算。
```

### 12. 管理员功能

步骤：

1. 使用管理员账号登录。
2. 进入管理员端。
3. 编辑商品、库存、秒杀时间、优惠券。
4. 尝试访问购物车或支付功能。

预期结果：

```text
管理员可以管理商品、库存、优惠券。
管理员不能进行购买、支付、加入购物车等普通用户操作。
```

## 六、接口测试

### 1. 登录并保存 Token

```powershell
$login = Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"student","password":"123456"}'

$token = $login.token
$h = @{Authorization="Bearer $token"}
```

### 2. 创建订单

```powershell
Invoke-RestMethod -Uri http://localhost/api/order/create `
  -Method POST `
  -ContentType 'application/json' `
  -Headers $h `
  -Body '{"productId":1,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学"}'
```

### 3. 查询我的订单

```powershell
Invoke-RestMethod -Uri http://localhost/api/order/mine -Headers $h
```

### 4. 秒杀下单

```powershell
Invoke-RestMethod -Uri http://localhost/api/seckill/order `
  -Method POST `
  -ContentType 'application/json' `
  -Headers $h `
  -Body '{"productId":4,"quantity":1,"receiverName":"测试用户","receiverPhone":"13800138000","address":"武汉大学"}'
```

## 七、高并发读测试

### 1. 商品详情缓存

多次请求：

```powershell
1..20 | ForEach-Object {
  Invoke-RestMethod -Uri http://localhost/api/product/1
}
```

预期结果：

```text
商品详情正常返回。
第一次可能访问数据库，后续优先命中 Redis。
```

### 2. 负载均衡

```powershell
1..10 | ForEach-Object {
  Invoke-RestMethod -Uri http://localhost/api/system/instance
}
```

预期结果：

```text
instanceId 在 app-1 和 app-2 之间切换。
```

## 八、高并发写测试

使用 JMeter 或并发脚本请求：

```text
POST http://localhost/api/seckill/order
```

观察：

```text
秒杀请求快速返回 QUEUED。
库存不会超卖。
同一用户同一商品只能秒杀一次。
seckill_transaction_log 状态最终为 SUCCESS 或 COMPENSATED。
```

## 九、服务治理测试

### 1. 管理员登录

```powershell
$admin = Invoke-RestMethod -Uri http://localhost/api/user/login `
  -Method POST `
  -ContentType 'application/json' `
  -Body '{"username":"admin","password":"admin123"}'

$adminHeaders = @{Authorization="Bearer $($admin.token)"}
```

### 2. 查看治理配置

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/config -Headers $adminHeaders
```

### 3. 修改限流阈值

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/config `
  -Method POST `
  -ContentType 'application/json' `
  -Headers $adminHeaders `
  -Body '{"key":"traffic.rate-limit.permits-per-second","value":"5"}'
```

### 4. 验证限流

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
部分请求返回 429。
```

### 5. 查看治理状态

```powershell
Invoke-RestMethod -Uri http://localhost/api/governance/traffic/status -Headers $adminHeaders
```

## 十、JMeter 测试

已有脚本：

```text
jmeter/api-load-test.jmx
jmeter/static-load-test.jmx
```

测试建议：

```text
静态资源：http://localhost/shop.html
商品接口：http://localhost/api/product
实例探针：http://localhost/api/system/instance
秒杀接口：http://localhost/api/seckill/order
```

关注指标：

- 平均响应时间。
- TPS。
- 错误率。
- Nginx 后端实例分布。
- 限流 429 比例。
- 熔断 503 比例。
- 秒杀事务日志最终状态。

## 十一、异常测试

| 场景 | 预期 |
| --- | --- |
| 未登录加入购物车 | 弹出登录窗口 |
| 管理员加入购物车 | 拒绝访问 |
| 库存不足下单 | 返回库存不足 |
| 重复秒杀 | 返回同一用户同一商品只能秒杀一次 |
| 订单超过 15 分钟未支付 | 自动取消 |
| 支付已取消订单 | 不允许支付 |
| 删除其他用户地址 | 不允许或不可见 |

## 十二、测试结论

系统应通过普通商城流程、秒杀流程、缓存验证、负载均衡验证、限流熔断验证和管理员权限验证。若以上测试全部符合预期，则系统满足课程作业和演示验收要求。
