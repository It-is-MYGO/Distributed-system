package com.example.distributedsystem.controller;

import com.example.distributedsystem.mapper.SeckillTransactionLogMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemCapabilityController {
    @Value("${app.instance-id:local}")
    private String instanceId;

    @Value("${server.port:8080}")
    private String serverPort;

    private final SeckillTransactionLogMapper seckillTransactionLogMapper;

    public SystemCapabilityController(SeckillTransactionLogMapper seckillTransactionLogMapper) {
        this.seckillTransactionLogMapper = seckillTransactionLogMapper;
    }

    @GetMapping("/instance")
    public ResponseEntity<Map<String, Object>> instance() {
        return ResponseEntity.ok(Map.of(
                "instanceId", instanceId,
                "port", serverPort,
                "gatewayHint", "通过 http://localhost/api/system/instance 多次请求可观察 Nginx 动态路由到不同实例"
        ));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> transactions() {
        return ResponseEntity.ok(Map.of(
                "scene", "秒杀下单：Redis预扣库存 + Kafka异步消息 + 本地订单事务 + 失败补偿",
                "statusMeaning", Map.of(
                        "PENDING", "Redis库存预扣成功，等待投递消息",
                        "QUEUED", "消息已投递到Kafka",
                        "PROCESSING", "消费者正在创建订单",
                        "SUCCESS", "订单与库存扣减最终一致",
                        "COMPENSATED", "订单创建失败，Redis库存和用户幂等锁已回滚"
                ),
                "records", seckillTransactionLogMapper.findRecent(30)
        ));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> capabilities() {
        return ResponseEntity.ok(Map.of(
                "instanceId", instanceId,
                "done", List.of(
                        "Docker Compose 容器化 MySQL/Redis/Kafka/Nginx/双后端实例",
                        "Nginx 静态资源托管与 /api 动态请求反向代理",
                        "Nginx round-robin 与 least_conn 两种负载均衡入口",
                        "Redis 商品详情缓存，包含空值缓存、互斥锁、随机 TTL 防穿透/击穿/雪崩",
                        "Redis 秒杀库存预扣与同一用户同一商品幂等限制",
                        "Kafka 秒杀订单异步创建，削峰填谷",
                        "秒杀事务日志：记录 PENDING/QUEUED/PROCESSING/SUCCESS/COMPENSATED，失败时回补 Redis 库存与用户锁",
                        "API 层限流、熔断、降级，可通过治理配置接口运行时更新",
                        "Nacos standalone 容器环境与服务注册/配置治理视图",
                        "雪花式 ID 生成订单号与秒杀请求号",
                        "库存表原子扣减，保证最终库存不超卖"
                ),
                "partialOrOptional", List.of(
                        "读写分离：当前 Docker 只有单 MySQL，代码和页面保留能力说明；真正主从需增加 mysql-master/mysql-slave 后接入动态数据源",
                        "Elasticsearch：作业标注可选，当前搜索使用 MySQL LIKE",
                        "ShardingSphere 分库分表：作业标注选做，当前未接入中间件"
                )
        ));
    }
}
