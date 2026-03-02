# 方案设计：基金详情页实时数据获取

## 1. 问题背景

当前基金详情页存在数据缺失问题：
- 当数据库中无指标信息、走势信息或基金基本信息时，直接返回空值/404
- 未触发后台数据补全机制，用户体验差
- 无缓存机制，可能重复查询 Python 服务

关联 Issue: `2026-03-02-002-fund-detail-data-source.md`

## 2. 目标定义

实现基金详情页的**实时数据补全机制**：
- 本地数据缺失时，自动调用 Python 采集服务获取
- 同步等待采集完成，返回完整数据
- 对用户透明，无需感知数据补全过程
- **新增**: Python 查询结果缓存 30 分钟，防止重复查询
- **新增**: 处理 Python 端也无法获取数据的场景

## 3. 技术方案

### 3.1 架构设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   用户请求   │────▶│  Java服务   │────▶│   本地数据库     │
└─────────────┘     └──────┬──────┘     └─────────────────┘
                           │
                    数据缺失？│
                           │ 否 - 直接返回
                           │
               ┌───────────┴───────────┐
               │                       │
               ▼                       ▼
    ┌─────────────────┐     ┌─────────────────┐
    │  查询缓存标记    │     │  缓存中存在     │
    │  (近期查询过?)   │     │  "无数据"标记?  │
    └────────┬────────┘     └────────┬────────┘
             │                       │
         是  │                       │ 是
             ▼                       ▼
    ┌─────────────────┐     ┌─────────────────┐
    │  直接返回空值   │     │  直接返回空值   │
    │  (不再查询Python)│     │  (不再查询Python)│
    └─────────────────┘     └─────────────────┘
             │
         否  │
             ▼
    ┌─────────────────┐     ┌─────────────────┐
    │  调用Python采集  │────▶│   采集服务      │
    │   (异步+轮询)   │◀────│  (异步采集)     │
    └────────┬────────┘     └─────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌──────────┐    ┌──────────┐
│ 采集成功  │    │ 采集失败  │
│ (存入DB)  │    │ (缓存空标记)│
└────┬─────┘    └────┬─────┘
     │               │
     ▼               ▼
┌──────────┐    ┌──────────┐
│返回完整数据│   │返回空值+提示│
└──────────┘    └──────────┘
```

### 3.2 核心流程

```
getFundDetail(fundCode):
    1. 查询本地基金基本信息
    2. 若不存在:
       a. 检查缓存标记 (key: "fund:empty:{code}")
       b. 若存在 → 直接返回空
       c. 调用 Python /api/collect/fund/{code}
       d. 若采集成功 → 存入DB，返回数据
       e. 若采集失败 → 缓存空标记(30min)，返回空
    3. 同理处理指标和NAV走势
    4. 组装并返回 FundDetailVO
```

### 3.3 缓存设计

```java
// Redis 缓存策略
Cache Strategy:
  
  # 1. 正常数据缓存 (原有)
  fund:info:{code}       - 基金信息 (TTL: 5min)
  fund:metrics:{code}    - 指标数据 (TTL: 5min)
  fund:nav:{code}        - NAV走势 (TTL: 5min)
  fund:estimate:{code}   - 实时估值 (TTL: 1min)
  
  # 2. 空值防护缓存 (新增)
  fund:empty:info:{code}     - 基金信息缺失标记 (TTL: 30min)
  fund:empty:metrics:{code}  - 指标缺失标记 (TTL: 30min)
  fund:empty:nav:{code}      - NAV缺失标记 (TTL: 30min)
```

### 3.4 Python 服务接口设计

```java
// CollectClient 新增方法
interface CollectClient {
    // 采集基金基本信息
    CollectResult<FundInfo> collectFundInfo(String fundCode);
    
    // 采集基金指标
    CollectResult<FundMetrics> collectFundMetrics(String fundCode);
    
    // 采集NAV历史
    CollectResult<List<NavData>> collectNavHistory(String fundCode);
}

// 采集结果
class CollectResult<T> {
    private boolean success;    // 是否成功
    private T data;             // 数据 (成功时有值)
    private String errorCode;   // 错误码 (失败时)
    private String message;     // 错误信息
    
    // 错误码定义
    public static final String ERR_FUND_NOT_FOUND = "FUND_NOT_FOUND";  // 基金不存在
    public static final String ERR_TIMEOUT = "TIMEOUT";                // 采集超时
    public static final String ERR_SERVICE_ERROR = "SERVICE_ERROR";    // 服务异常
}
```

### 3.5 轮询机制

```yaml
# application-collect.yml
collect:
  poll:
    interval: 500ms      # 轮询间隔
    max-attempts: 30     # 最大轮询次数 (15s)
    timeout: 15s         # 总超时时间
  cache:
    empty-ttl: 30m       # 空值缓存时间
```

### 3.6 空值处理策略

```java
@Service
public class FundDataFetchService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String EMPTY_PREFIX = "fund:empty:";
    private static final Duration EMPTY_TTL = Duration.ofMinutes(30);
    
    /**
     * 获取基金信息（带缓存和空值防护）
     */
    public FundInfo getFundInfo(String fundCode) {
        // 1. 查本地DB
        FundInfo fundInfo = fundInfoMapper.selectById(fundCode);
        if (fundInfo != null) {
            return fundInfo;
        }
        
        // 2. 检查空值缓存（近期查询过且确定不存在）
        String emptyKey = EMPTY_PREFIX + "info:" + fundCode;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(emptyKey))) {
            log.debug("基金[{}]近期已确认不存在，跳过查询", fundCode);
            return null;
        }
        
        // 3. 调用Python服务采集
        CollectResult<FundInfo> result = collectClient.collectFundInfo(fundCode);
        
        if (result.isSuccess()) {
            // 采集成功，存入DB
            fundInfo = result.getData();
            fundInfoMapper.insert(fundInfo);
            return fundInfo;
        } else {
            // 采集失败，缓存空值标记
            if (CollectResult.ERR_FUND_NOT_FOUND.equals(result.getErrorCode())) {
                log.warn("基金[{}]在Python端也不存在", fundCode);
            }
            redisTemplate.opsForValue().set(emptyKey, "1", EMPTY_TTL);
            return null;
        }
    }
    
    // 同理实现 getMetrics(), getNavHistory()
}
```

## 4. 实现细节

### 4.1 修改文件清单

| 序号 | 文件路径 | 修改内容 |
|------|----------|----------|
| 1 | `client/CollectClient.java` | 新增采集接口方法 |
| 2 | `client/CollectResult.java` | 新增采集结果封装类 |
| 3 | `service/FundDataFetchService.java` | **新增**: 带缓存的数据获取服务 |
| 4 | `service/FundService.java` | 集成 FundDataFetchService |
| 5 | `config/CollectProperties.java` | 轮询和缓存配置参数 |
| 6 | `FundDataFetchServiceTest.java` | 单元测试 |

### 4.2 异常处理与降级

```java
@Component
public class CollectFallback {
    
    /**
     * Python服务不可用时的降级处理
     */
    public FundInfo fallbackFundInfo(String fundCode, Exception ex) {
        log.error("Python采集服务异常，基金[{}]降级处理", fundCode, ex);
        // 返回空，前端显示"数据准备中"提示
        return null;
    }
}
```

### 4.3 并发控制

```java
@Service
public class FundDataFetchService {
    
    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingTasks = 
        new ConcurrentHashMap<>();
    
    /**
     * 防止并发重复采集同一基金
     */
    public FundInfo getFundInfoWithLock(String fundCode) {
        // 检查是否已有正在进行的采集任务
        CompletableFuture<FundInfo> future = pendingTasks.computeIfAbsent(
            fundCode,
            k -> CompletableFuture.supplyAsync(() -> doCollectFundInfo(k))
        );
        
        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("采集基金[{}]失败", fundCode, e);
            return null;
        } finally {
            pendingTasks.remove(fundCode);
        }
    }
}
```

## 5. 风险评估

| 风险 | 等级 | 应对措施 |
|------|------|----------|
| Python服务不可用 | 高 | 降级策略：返回部分数据+友好提示 |
| 采集超时 | 中 | 配置合理超时时间(15s)，异步兜底 |
| 并发采集压力 | 中 | 添加采集任务去重/限流 |
| Python端数据也不存在 | 中 | 30分钟空值缓存，防止反复查询 |
| 数据一致性 | 低 | 事务保证，采集成功后刷新缓存 |

## 6. 测试策略

### 6.1 单元测试
- 模拟 Python 服务响应（成功/失败/超时）
- 测试空值缓存逻辑
- 测试并发采集去重
- 测试轮询逻辑

### 6.2 集成测试
- 真实调用 Python 服务
- 验证缓存生效（30分钟内不重复查询）
- 验证完整数据流
- 性能测试（响应时间 < 5s）

### 6.3 边界测试
- 基金代码不存在（Python端也查不到）
- Python服务超时
- 并发请求同一基金
- Redis缓存失效

## 7. 验收标准

- [ ] 数据库无数据时，自动触发采集
- [ ] 采集完成后返回完整数据
- [ ] Python端无数据时，缓存30分钟空值标记
- [ ] 30分钟内不再重复查询Python
- [ ] 并发请求同一基金只触发一次采集
- [ ] Python服务异常时有降级处理
- [ ] 响应时间 < 5s（含采集）
- [ ] 单元测试覆盖率 > 80%

---
**文档版本**: v1.1  
**更新时间**: 2026-03-02 16:29  
**关联Issue**: 2026-03-02-002
