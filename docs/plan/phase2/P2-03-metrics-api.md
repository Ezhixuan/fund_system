# P2-03: 指标查询API - 执行计划

> 工期：4天 | 依赖：P2-01

---

## Day 1-2: 指标查询

### 实体与VO
```java
// entity/FundMetrics.java
@Data
@TableName("fund_metrics")
public class FundMetrics {
    private Long id;
    private String fundCode;
    private LocalDate calcDate;
    private BigDecimal return1m;
    private BigDecimal return3m;
    private BigDecimal return1y;
    private BigDecimal return3y;
    private BigDecimal sharpeRatio1y;
    private BigDecimal maxDrawdown1y;
    private BigDecimal volatility1y;
    private BigDecimal sortinoRatio1y;
    private BigDecimal calmarRatio3y;
    private BigDecimal alpha1y;
    private BigDecimal beta1y;
    private Integer pePercentile;
}

// dto/FundMetricsVO.java
@Data
public class FundMetricsVO {
    private String fundCode;
    private String fundName;
    private LocalDate calcDate;
    private BigDecimal return1y;
    private BigDecimal return3y;
    private BigDecimal sharpeRatio;
    private BigDecimal maxDrawdown;
    private BigDecimal volatility;
    private String qualityLevel;  // S/A/B/C/D
}
```

### Service实现
```java
@Service
public class MetricsServiceImpl implements MetricsService {
    
    @Autowired
    private FundMetricsMapper metricsMapper;
    
    @Override
    public FundMetricsVO getLatestMetrics(String fundCode) {
        FundMetrics metrics = metricsMapper.selectLatestByFundCode(fundCode);
        return convertToVO(metrics);
    }
    
    @Override
    public List<FundMetricsVO> getTopFunds(String type, int limit) {
        return metricsMapper.selectTopBySharpe(type, limit)
            .stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
}
```

---

## Day 3-4: 净值历史与排名

### 净值历史查询
```java
// entity/FundNav.java
@Data
@TableName("fund_nav")
public class FundNav {
    private Long id;
    private String fundCode;
    private LocalDate navDate;
    private BigDecimal unitNav;
    private BigDecimal accumNav;
    private BigDecimal dailyReturn;
}

// Service
public List<FundNav> getNavHistory(String fundCode, LocalDate start, LocalDate end) {
    LambdaQueryWrapper<FundNav> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(FundNav::getFundCode, fundCode)
           .ge(FundNav::getNavDate, start)
           .le(FundNav::getNavDate, end)
           .orderByAsc(FundNav::getNavDate);
    return navMapper.selectList(wrapper);
}
```

### Controller
```java
@RestController
@RequestMapping("/api/funds")
public class MetricsController {
    
    @Autowired
    private MetricsService metricsService;
    
    @GetMapping("/{code}/metrics")
    public ApiResponse<FundMetricsVO> metrics(@PathVariable String code) {
        return ApiResponse.success(metricsService.getLatestMetrics(code));
    }
    
    @GetMapping("/{code}/nav")
    public ApiResponse<List<FundNav>> navHistory(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.success(metricsService.getNavHistory(code, start, end));
    }
    
    @GetMapping("/top")
    public ApiResponse<List<FundMetricsVO>> topFunds(
            @RequestParam(defaultValue = "sharpe") String by,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(metricsService.getTopFunds(by, limit));
    }
}
```

---

## 验收清单
- [ ] GET /api/funds/{code}/metrics 返回全维指标
- [ ] GET /api/funds/{code}/nav 返回时间序列
- [ ] GET /api/funds/top 返回排名
- [ ] 响应时间<200ms
