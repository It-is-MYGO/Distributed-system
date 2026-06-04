# 商品库存与秒杀系统作业文档目录

本目录按课程作业要求拆分为 5 份提交文档，每份文档都包含：作业要求、完成情况、系统实现、关键代码和未完成/选做说明。

## 文档列表

0. [作业总报告](./00-submission-report.md)
1. [作业一：商品库存与秒杀系统设计](./01-system-design.md)
2. [作业二：高并发读](./02-high-concurrency-read.md)
3. [作业三：高并发写](./03-high-concurrency-write.md)
4. [作业四：分布式事务](./04-distributed-transaction.md)
5. [作业五：服务治理](./05-service-governance.md)
6. [附录：启动与验证手册](./appendix-run-and-verify.md)

## 项目技术栈

- 后端：Spring Boot、Spring Security、JWT、MyBatis
- 数据库：MySQL
- 缓存：Redis
- 消息队列：Kafka
- 服务治理：Nacos、运行时治理配置、限流、熔断、降级
- 网关与负载均衡：Nginx
- 容器化：Docker、Docker Compose
- 压测：JMeter
- 前端：HTML、CSS、JavaScript

## 说明

当前项目已经完成商城基本功能、商品库存管理、秒杀下单、订单支付、购物车、优惠券、评价、物流模拟、用户消息邮箱、管理员管理等业务功能。

课程作业中的核心高并发要求已经实现：Redis 缓存、Redis 秒杀库存预扣、Kafka 异步下单、数据库库存原子扣减、Nginx 双实例负载均衡、JMeter 压测脚本、Nacos 配置治理、限流和熔断。

未实现或选做说明：

- MySQL 读写分离未接入，当前为单 MySQL 实例。
- Elasticsearch 未接入，当前搜索使用 MySQL LIKE 实现；题目中 Elasticsearch 标注为可选。
- ShardingSphere 分库分表未接入；题目中分库分表标注为选做。
- Spring Cloud Gateway 未接入，当前使用 Nginx 实现统一入口和负载均衡。
