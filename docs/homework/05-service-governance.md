# 作业五：服务治理

## 一、作业要求

1. 搭建 Nacos 环境，实现服务注册、配置管理。
2. 结合 Spring Cloud Gateway 服务网关。
3. 使用网关地址调用服务，测试动态服务路由正确性。
4. 在代码中使用 Nacos 属性，测试动态更新属性能力。
5. 对服务设置熔断、限流和降级。
6. 使用 JMeter 压力测试，验证流量治理效果。

## 二、完成情况

已完成：Nacos 容器环境、服务注册、心跳、配置发布、配置拉取、动态配置、限流、熔断、降级、JMeter 压测脚本。

部分替代实现：没有接入 Spring Cloud Gateway，当前使用 Nginx 作为统一网关入口和负载均衡器。

## 三、Nacos 环境

`docker-compose.yml` 中配置了 Nacos：

```yaml
nacos:
  image: nacos/nacos-server:v2.3.2
  container_name: ds-nacos
  environment:
    MODE: standalone
    NACOS_AUTH_ENABLE: "false"
  ports:
    - "8849:8848"
    - "9849:9848"
```

访问地址：

```text
http://localhost:8849/nacos
```

后端 Docker 配置：

```yaml
app1:
  environment:
    NACOS_SERVER_ADDR: http://nacos:8848

app2:
  environment:
    NACOS_SERVER_ADDR: http://nacos:8848
```

## 四、服务注册

服务启动后会向 Nacos 注册实例，代码位于 `NacosGovernanceService`：

```java
private void registerInstance() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("serviceName", SERVICE_NAME);
    form.add("ip", instanceIp);
    form.add("port", String.valueOf(serverPort));
    form.add("ephemeral", "true");
    form.add("metadata", "{\"instanceId\":\"" + instanceId + "\"}");
    restTemplate.postForObject(nacosServer + "/nacos/v1/ns/instance", form, String.class);
}
```

服务名：

```java
private static final String SERVICE_NAME = "mall-service";
```

两个后端实例：

```text
app-1 -> app1:8081
app-2 -> app2:8082
```

## 五、服务心跳

服务每 5 秒向 Nacos 发送心跳：

```java
@Scheduled(fixedDelay = 5000, initialDelay = 8000)
public void heartbeat() {
    String beat = """
        {"serviceName":"%s","ip":"%s","port":%d,"cluster":"DEFAULT","metadata":{"instanceId":"%s"}}
        """.formatted(SERVICE_NAME, instanceIp, serverPort, instanceId);

    restTemplate.put(URI.create(url), null);
}
```

## 六、配置管理

系统启动时会向 Nacos 发布默认治理配置：

```java
String content = """
traffic.rate-limit.enabled=true
traffic.rate-limit.permits-per-second=35
traffic.circuit-breaker.enabled=true
traffic.circuit-breaker.failure-threshold=8
traffic.circuit-breaker.open-seconds=20
traffic.degrade.enabled=true
traffic.degrade.path-prefix=/api/product/search
""";
```

发布到 Nacos：

```java
restTemplate.postForObject(nacosServer + "/nacos/v1/cs/configs", form, String.class);
```

## 七、动态配置拉取

后端定时从 Nacos 拉取配置并更新本地治理配置：

```java
@Scheduled(fixedDelay = 15000, initialDelay = 10000)
public void syncConfigFromNacos() {
    String content = restTemplate.getForObject(URI.create(url), String.class);
    Arrays.stream(content.split("\\R"))
        .filter(line -> !line.isBlank() && line.contains("="))
        .forEach(line -> {
            String[] parts = line.split("=", 2);
            governanceConfigService.update(parts[0].trim(), parts[1].trim());
        });
}
```

说明：修改 Nacos 中的限流、熔断参数后，后端会定时同步，不需要重启服务。

## 八、治理接口

管理员可以通过治理接口查看和更新配置：

```java
@GetMapping("/config")
public ResponseEntity<List<GovernanceConfig>> listConfig()

@PostMapping("/config")
public ResponseEntity<GovernanceConfig> updateConfig()

@GetMapping("/service-registry")
public ResponseEntity<Map<String, Object>> serviceRegistry()

@GetMapping("/traffic/status")
public ResponseEntity<Map<String, Object>> trafficStatus()
```

更新配置后会同步发布到 Nacos：

```java
GovernanceConfig config = configService.update(request.getKey(), request.getValue());
trafficGovernanceFilter.reset();
nacosConfigService.publish(configService.list());
```

## 九、网关路由

题目要求 Spring Cloud Gateway，当前项目使用 Nginx 替代实现统一入口和负载均衡：

```nginx
upstream backend_round_robin {
    server app1:8081;
    server app2:8082;
}

location /api/ {
    proxy_pass http://backend_round_robin;
}
```

统一访问地址：

```text
http://localhost/api/**
```

动态路由验证：

```powershell
1..8 | ForEach-Object {
  Invoke-RestMethod -Uri http://localhost/api/system/instance
}
```

多次请求会看到实例在 `app-1` 和 `app-2` 之间变化。

## 十、限流

`TrafficGovernanceFilter` 使用令牌桶算法实现限流：

```java
if (configService.bool("traffic.rate-limit.enabled", true)) {
    int permits = Math.max(1,
        configService.integer("traffic.rate-limit.permits-per-second", 35));

    TokenBucket bucket = buckets.computeIfAbsent(path, ignored -> new TokenBucket(permits));
    bucket.resize(permits);

    if (!bucket.tryAcquire()) {
        writeGovernanceResponse(response, 429, "当前访问过于频繁，已触发限流", "RATE_LIMITED");
        return;
    }
}
```

令牌桶：

```java
synchronized boolean tryAcquire() {
    if (now - lastRefillMillis >= 1000) {
        tokens = capacity;
        lastRefillMillis = now;
    }
    if (tokens <= 0) {
        return false;
    }
    tokens--;
    return true;
}
```

触发限流时返回：

```json
{"code":"RATE_LIMITED","message":"当前访问过于频繁，已触发限流"}
```

## 十一、熔断

当后端连续失败次数达到阈值时打开熔断：

```java
private void recordFailure(String path) {
    int threshold = Math.max(1,
        configService.integer("traffic.circuit-breaker.failure-threshold", 8));
    int failures = failureCounter.incrementAndGet();

    if (failures >= threshold) {
        int openSeconds = Math.max(1,
            configService.integer("traffic.circuit-breaker.open-seconds", 20));
        circuitOpenUntil = Instant.now().plusSeconds(openSeconds).toEpochMilli();
        failureCounter.set(0);
    }
}
```

熔断打开时返回：

```java
writeGovernanceResponse(response, 503, "服务熔断中，请稍后重试", "CIRCUIT_OPEN");
```

## 十二、降级

系统配置了降级路径：

```java
String degradePrefix = configService.get("traffic.degrade.path-prefix", "/api/product/search");
if (configService.bool("traffic.degrade.enabled", true)
        && path.startsWith(degradePrefix)) {
    circuitOpenUntil = Math.max(circuitOpenUntil,
        Instant.now().plusSeconds(3).toEpochMilli());
}
```

默认对商品搜索接口进行降级保护。

## 十三、JMeter 压测

已有压测脚本：

```text
jmeter/api-load-test.jmx
jmeter/static-load-test.jmx
```

推荐压测接口：

```text
GET  /api/product
GET  /api/product/search?keyword=手机
POST /api/seckill/order
GET  /api/system/instance
```

验证点：

- 请求是否分布到 `app-1` 和 `app-2`。
- 降低限流阈值后是否返回 `429 RATE_LIMITED`。
- 连续异常后是否返回 `503 CIRCUIT_OPEN`。
- 秒杀高并发下事务日志是否最终为 `SUCCESS` 或 `COMPENSATED`。

## 十四、结论

本作业已经完成 Nacos 服务注册、配置管理、动态配置、限流、熔断、降级和压测脚本。唯一替代点是未使用 Spring Cloud Gateway，而是使用 Nginx 实现统一入口和负载均衡。
