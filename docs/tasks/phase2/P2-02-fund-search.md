# Task: P2-02-fund-search

## 任务信息
- **任务ID**: P2-02
- **任务名称**: 基金检索API
- **所属阶段**: Phase 2 后端核心层
- **计划工期**: 3天
- **实际工期**: 0.5天
- **创建时间**: 2026-02-28
- **完成时间**: 2026-02-28
- **状态**: ✅ 已完成
- **工作分支**: service-core
- **前置依赖**: P2-01 ✅

## API清单

### 1. 基金列表查询
```
GET /api/funds?page=1&size=20&fundType=混合型&riskLevel=3&keyword=华夏
```
响应: `ApiResponse<IPage<FundInfoVO>>`

### 2. 基金详情查询
```
GET /api/funds/{fundCode}
```
响应: `ApiResponse<FundInfoVO>`

### 3. 搜索建议
```
GET /api/funds/search/suggest?keyword=xxx&limit=10
```
响应: `ApiResponse<List<FundInfoVO>>`

### 4. 基金指标
```
GET /api/funds/{fundCode}/metrics
```
响应: `ApiResponse<FundMetricsVO>`

### 5. 净值历史
```
GET /api/funds/{fundCode}/nav?startDate=2024-01-01&endDate=2024-12-31
```
响应: `ApiResponse<List<FundNavVO>>`

### 6. 近期净值
```
GET /api/funds/{fundCode}/nav/recent?days=30
```
响应: `ApiResponse<List<FundNavVO>>`

## 数据结构

### FundInfoVO
```java
public class FundInfoVO {
    private String fundCode;      // 基金代码
    private String fundName;      // 基金名称
    private String namePinyin;    // 名称拼音
    private String fundType;      // 基金类型
    private String investStyle;   // 投资风格
    private String managerName;   // 基金经理
    private String companyName;   // 基金公司
    private Integer riskLevel;    // 风险等级
    private LocalDate establishDate;  // 成立日期
    private BigDecimal currentScale;  // 当前规模
    private String benchmark;     // 业绩基准
}
```

### FundMetricsVO
```java
public class FundMetricsVO {
    private String fundCode;
    private String fundName;
    private LocalDate calcDate;
    private BigDecimal return1m;      // 近1月收益
    private BigDecimal return3m;      // 近3月收益
    private BigDecimal return1y;      // 近1年收益
    private BigDecimal return3y;      // 近3年收益
    private BigDecimal sharpeRatio1y; // 夏普比率
    private BigDecimal maxDrawdown1y; // 最大回撤
    private BigDecimal volatility1y;  // 波动率
    private String qualityLevel;      // 质量等级 S/A/B/C/D
}
```

### FundNavVO
```java
public class FundNavVO {
    private LocalDate navDate;      // 净值日期
    private BigDecimal unitNav;     // 单位净值
    private BigDecimal accumNav;    // 累计净值
    private BigDecimal dailyReturn; // 日收益率
}
```

## 实现文件

### VO层
- `FundInfoVO.java` - 基金信息VO
- `FundMetricsVO.java` - 基金指标VO
- `FundNavVO.java` - 基金净值VO

### Service层
- `FundService.java` - 服务接口
- `FundServiceImpl.java` - 服务实现

### Controller层
- `FundController.java` - REST API控制器

## 技术要点

1. **MyBatis-Plus分页**: 使用IPage实现分页查询
2. **多字段搜索**: 支持名称、代码、拼音联合搜索
3. **VO转换**: Entity与VO分离，保护内部数据结构
4. **质量等级**: 根据夏普比率自动计算S/A/B/C/D等级

## 测试验证

```bash
# 健康检查
curl http://localhost:8080/health

# 基金列表
curl "http://localhost:8080/api/funds?page=1&size=5"

# 基金详情
curl http://localhost:8080/api/funds/000001

# 搜索建议
curl "http://localhost:8080/api/funds/search/suggest?keyword=华夏&limit=3"

# 近期净值
curl "http://localhost:8080/api/funds/000001/nav/recent?days=30"
```

## Git提交
```
feat(api): P2-02 基金检索API
- 基金列表分页查询
- 基金详情查询
- 搜索建议API
- 基金指标查询
- 净值历史查询
- VO对象封装
- FundService实现
```

## 下一步
- P2-03: 指标计算API
- P2-04: Redis缓存集成
