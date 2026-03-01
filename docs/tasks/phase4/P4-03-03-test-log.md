# P4-03-03: 性能优化-压测与调优 - 测试日志

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-03-03 |
| 名称 | 性能优化-压测与调优 |
| 执行日期 | 2026-03-01 |
| 执行者 | OpenClaw |

---

## 实施内容

### 1. 压测工具
- ✅ 创建 `loadtest.sh` - 基础压力测试脚本
- ✅ 创建 `benchmark.sh` - 详细基准测试脚本

### 2. 监控增强
- ✅ 添加 Spring Boot Actuator 依赖
- ✅ 添加 Micrometer Prometheus 支持
- ✅ 创建 `MetricsService.java` - 性能指标记录服务
- ✅ 创建 `PerfTestController.java` - 性能测试报告接口

### 3. 配置优化

#### Tomcat连接池优化
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 10000
    accept-count: 100
```

#### Hikari连接池优化
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 根据并发量调整
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000
```

#### Redis连接池优化
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 4
```

#### 异步线程池优化
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 200
```

### 4. JVM调优配置
- ✅ 创建 `jvm-options.sh` - JVM参数配置脚本
- 使用 G1 垃圾收集器（Java 17推荐）
- 配置 GC 日志输出
- 配置 OOM 堆转储
- 启用字符串去重

---

## 新增文件

```
fund-service/
├── pom.xml                              # 添加Actuator和Micrometer依赖
├── src/main/resources/application.yml   # 优化连接池和线程池配置
├── src/main/java/com/fund/
│   ├── service/MetricsService.java      # 性能指标服务
│   └── controller/PerfTestController.java # 性能测试报告接口
└── scripts/
    ├── loadtest.sh                      # 压力测试脚本
    ├── benchmark.sh                     # 基准测试脚本
    └── jvm-options.sh                   # JVM参数配置
```

---

## 性能监控API

### 性能概览
```
GET /admin/perf/overview
```
返回：
- JVM内存使用情况
- CPU使用率
- 线程数
- GC次数
- 总请求数

### JVM详细指标
```
GET /admin/perf/jvm
```
返回：
- 堆内存使用
- 非堆内存使用
- GC统计
- 线程统计

### 数据源连接池
```
GET /admin/perf/datasource
```
返回：
- 活跃连接数
- 空闲连接数
- 最大连接数
- 最小空闲连接

### 所有指标列表
```
GET /admin/perf/metrics
GET /actuator/metrics
GET /actuator/prometheus
```

---

## 压测脚本使用

### 快速压测
```bash
cd fund-service/scripts
./loadtest.sh http://localhost:8080 50 1000
```
参数：
- $1: 基准URL（默认: http://localhost:8080）
- $2: 并发数（默认: 50）
- $3: 总请求数（默认: 1000）

### 基准测试
```bash
./benchmark.sh http://localhost:8080
```
生成详细的压测报告到 `/tmp/fund-benchmark-{timestamp}/`

---

## JVM启动参数

### 开发环境
```bash
./jvm-options.sh dev
```

### 生产环境
```bash
export JAVA_OPTS=$(./jvm-options.sh prod | grep -A100 "JVM参数:" | tail -n +2 | tr '\n' ' ')
java $JAVA_OPTS -jar fund-service.jar
```

---

## 性能目标与调优建议

### 目标指标
| 指标 | 目标值 | 调优方向 |
|------|--------|----------|
| QPS | > 100 | 增加连接池、优化缓存 |
| P99 | < 200ms | SQL优化、索引、缓存 |
| 错误率 | < 0.1% | 超时配置、熔断降级 |

### 调优建议

1. **如果QPS不达标**
   - 增加 Tomcat 线程数（max: 200 → 400）
   - 增加数据库连接池（max: 20 → 50）
   - 增加 Redis 连接池（max-active: 16 → 32）
   - 启用更多缓存

2. **如果P99延迟过高**
   - 检查慢 SQL，添加索引
   - 增加 Redis 缓存命中率
   - 优化序列化（使用Protobuf或Kryo）
   - 启用异步处理

3. **如果错误率高**
   - 调整连接超时时间
   - 添加熔断降级（Resilience4j）
   - 增加限流保护

---

## 编译测试

```bash
mvn clean compile -q
```

**结果**: ✅ 编译成功

---

## Git提交

```bash
git add .
git commit -m "perf(benchmark): 添加压测工具和性能监控

- 添加压力测试脚本(loadtest.sh, benchmark.sh)
- 添加Actuator和Micrometer性能监控
- 优化Tomcat、Hikari、Redis连接池配置
- 优化异步线程池配置
- 添加性能监控API(/admin/perf/*)
- 添加JVM调优配置脚本

任务: P4-03-03"
```

---

**测试完成时间**: 2026-03-01 23:05
