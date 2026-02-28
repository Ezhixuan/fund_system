# P2-04: Redis缓存集成 - 执行计划

> 工期：2天 | 依赖：P2-02,P2-03

---

## Day 1: Redisson配置

### 配置类
```java
// config/RedisConfig.java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://localhost:6379")
              .setDatabase(0)
              .setConnectionMinimumIdleSize(5)
              .setConnectionPoolSize(10);
        return Redisson.create(config);
    }
    
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        
        // 基金信息缓存 5分钟
        config.put("fund", new CacheConfig(5 * 60 * 1000, 0));
        
        // 指标缓存 5分钟
        config.put("metrics", new CacheConfig(5 * 60 * 1000, 0));
        
        // 排名缓存 1小时
        config.put("ranking", new CacheConfig(60 * 60 * 1000, 0));
        
        return new RedissonSpringCacheManager(redissonClient, config);
    }
}
```

---

## Day 2: 缓存应用

### Service层加缓存
```java
@Service
public class FundServiceImpl implements FundService {
    
    @Override
    @Cacheable(value = "fund", key = "#code")
    public FundVO getFundDetail(String code) {
        // 查询数据库
        return fundInfoMapper.selectById(code);
    }
    
    @Override
    @CacheEvict(value = "fund", key = "#code")
    public void updateFund(String code, FundUpdateRequest request) {
        // 更新数据库
    }
}

@Service
public class MetricsServiceImpl implements MetricsService {
    
    @Override
    @Cacheable(value = "metrics", key = "#fundCode")
    public FundMetricsVO getLatestMetrics(String fundCode) {
        return metricsMapper.selectLatestByFundCode(fundCode);
    }
    
    @Override
    @Cacheable(value = "ranking", key = "#type + '-' + #limit")
    public List<FundMetricsVO> getTopFunds(String type, int limit) {
        return metricsMapper.selectTopBySharpe(type, limit);
    }
}
```

### 缓存监控
```java
@RestController
@RequestMapping("/admin/cache")
public class CacheMonitorController {
    
    @Autowired
    private RedissonClient redissonClient;
    
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取缓存统计
        RMap<String, String> fundCache = redissonClient.getMap("fund");
        stats.put("fund_cache_size", fundCache.size());
        
        return ApiResponse.success(stats);
    }
    
    @PostMapping("/clear")
    public ApiResponse<Void> clear(@RequestParam String name) {
        redissonClient.getKeys().deleteByPattern(name + "*");
        return ApiResponse.success(null);
    }
}
```

---

## 验收清单
- [ ] 缓存正常写入Redis
- [ ] 缓存命中率>80%
- [ ] 缓存更新及时
- [ ] 缓存穿透防护（布隆过滤器或空值缓存）
