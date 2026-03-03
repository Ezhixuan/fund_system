# Issue: Redisson 缓存格式错误导致系统异常

**Created**: 2026-03-04 02:33
**Status**: Fixed
**Priority**: High
**Fixed At**: 2026-03-04 03:45

## Problem Description

系统运行时抛出 RedisException 异常：
```
org.redisson.client.RedisException: ERR user_script:1: bad argument #2 to 'unpack' (data string too short)
```

异常发生在访问 `fund:estimate` 缓存时，导致估值相关接口无法正常响应。

## Environment

- Project: fund-system
- Service: fund-service (Java)
- Cache: Redis 7.0 + Redisson 3.25.0
- Cache Key: `fund:estimate`
- Error Location: `@user_script:1` (Redisson 内部 Lua 脚本)

## Error Stack

```
org.redisson.client.RedisException: ERR user_script:1: bad argument #2 to 'unpack' (data string too short)
    script: a383653c28a2f1517924412ba236179ad89548b2
    channel: [id: 0x28eeb1a3, L:/127.0.0.1:50556 - R:127.0.0.1/127.0.0.1:6379]
    command: (EVAL)
    params: [local value = redis.call('hget', KEYS[1], ARGV[2]); ...]
```

## Analysis

### 错误原因

1. **数据格式不匹配**: Redis 中存储的缓存数据格式与 Redisson 期望的格式不一致
2. **Redisson Hash 结构**: Redisson 使用 Hash 结构存储数据，其中 value 使用 `struct.pack('dLc0', ...)` 格式打包
3. **数据损坏或过期**: 存储的数据可能因以下原因损坏：
   - 手动修改了 Redis 中的数据
   - Redis 中残留了旧版本应用程序写入的数据
   - 应用程序升级后缓存格式发生变化
   - Redis 实例被其他应用复用，key 冲突

### 技术细节

Redisson 的 RMap 使用以下格式存储数据：
```
value = struct.pack('dLc0', timestamp, length, data)
```

当读取数据时，使用 `struct.unpack('dLc0', value)` 解包。

错误 `data string too short` 表示：
- value 存在但长度不足
- value 不是 Redisson 打包的格式
- value 被截断或损坏

## Root Cause

Redis 中的 `fund:estimate` key 存储了格式不正确的数据，可能原因：

1. **应用重启前未清理缓存**: 升级应用版本后，旧格式缓存残留
2. **手动操作 Redis**: 使用 redis-cli 或其他工具直接修改了数据
3. **Key 冲突**: 其他应用或旧代码使用了相同的 key 名
4. **序列化配置变更**: 应用中的序列化配置（如 JSON/Kryo）发生变化

## Solution

### Option 1 (Recommended): 清除问题缓存

立即清除 `fund:estimate` 相关的 Redis 缓存：

```bash
# 使用 redis-cli
redis-cli -p 16379

# 删除问题 key
DEL fund:estimate

# 或者删除所有相关缓存
KEYS fund:* | xargs redis-cli DEL

# 清除 Redisson 内部使用的 timeout set
DEL redisson__timeout__set:{fund:estimate}
```

或通过 Java 代码清除：
```java
@Autowired
private RedissonClient redissonClient;

public void clearEstimateCache() {
    RMap<String, String> map = redissonClient.getMap("fund:estimate");
    map.delete();
    System.out.println("fund:estimate 缓存已清除");
}
```

### Option 2: 清除所有缓存（激进但有效）

```bash
# 清除整个 Redis 数据库（开发环境可用）
redis-cli -p 16379 FLUSHDB

# 或清除所有数据（包括其他 db）
redis-cli FLUSHALL
```

**⚠️ 警告**: 生产环境慎用，会清除所有缓存数据！

### Option 3: 修改缓存 Key 名称（避免冲突）

如果存在 key 命名冲突，修改 application.yml 中的缓存名称：

```yaml
# 修改前
cache:
  names:
    estimate: fund:estimate

# 修改后（添加版本号）
cache:
  names:
    estimate: fund:estimate:v2
```

### Option 4: 配置 Redisson 自动重建

在 Redisson 配置中启用自动重建：

```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          retryAttempts: 3
          retryInterval: 1500
        # 启用缓存统计，便于发现问题
        mapCache:
          fund:estimate:
            maxSize: 1000
            timeToLive: 3600000
```

## Action Items

- [x] **立即处理**: 清除 `fund:estimate` 缓存
  ```bash
  docker exec fund-redis redis-cli DEL 'fund:estimate' '{fund:estimate}:redisson_options'
  ```
- [x] **验证**: 重启应用后测试估值接口是否正常
- [x] **预防措施**: 在应用启动时添加缓存清理逻辑 (CacheCleaner.java)
- [ ] **监控**: 添加 Redis 缓存异常监控告警
- [ ] **文档**: 记录缓存 key 命名规范，避免冲突

## Fix Applied

**修复时间**: 2026-03-04 03:45

**执行内容**:
1. 清除 Redis 问题缓存 (fund:estimate 及相关 key)
2. 创建 CacheCleaner 组件，应用启动时自动清理缓存
3. 修改 EstimateService，添加缓存异常降级处理

**相关文档**:
- Task: [修复 Redisson 缓存格式错误](../task/bugfix/2026-03-04-redisson-cache-error.md)
- Plan: [修复 Redisson 缓存格式错误计划](../plan/bugfix/2026-03-04-redisson-cache-error.md)

## Prevention

### 1. 应用启动时清理缓存

```java
@Component
public class CacheCleaner implements CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void run(String... args) {
        // 清理可能存在格式问题的缓存
        String[] keysToClean = {"fund:estimate", "fund:detail", "fund:metrics"};
        for (String key : keysToClean) {
            try {
                RMap<?, ?> map = redissonClient.getMap(key);
                if (map != null && map.size() > 0) {
                    map.delete();
                    log.info("已清理缓存: {}", key);
                }
            } catch (Exception e) {
                log.warn("清理缓存 {} 失败: {}", key, e.getMessage());
            }
        }
    }
}
```

### 2. 添加缓存异常处理

```java
@Service
public class EstimateService {

    @Autowired
    @Qualifier("estimateCache")
    private Cache estimateCache;

    public Object getEstimate(String fundCode) {
        try {
            return estimateCache.get(fundCode);
        } catch (RedisException e) {
            log.error("缓存读取失败，清除后重试: {}", e.getMessage());
            estimateCache.clear();
            return null;
        }
    }
}
```

### 3. 使用不同的 Redis DB

```yaml
spring:
  redis:
    host: localhost
    port: 16379
    database: 1  # 使用 DB 1，避免与其他应用冲突
```

## Quick Fix

**立即执行以下命令解决问题：**

```bash
# 1. 连接 Redis
redis-cli -p 16379

# 2. 删除问题 key
DEL fund:estimate
DEL redisson__timeout__set:{fund:estimate}

# 3. 验证
KEYS fund:estimate*
# 应返回空或只有新创建的 key
```

然后重启 Java 应用。

## Related

- Redisson Issue: https://github.com/redisson/redisson/issues/xxx
- Redis 命令参考: https://redis.io/commands/del
- 应用配置: `fund-service/src/main/resources/application.yml`
- Redisson 配置: `RedisConfig.java`
