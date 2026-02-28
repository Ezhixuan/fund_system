# P2-03 测试日志

## 测试时间
2026-02-28 23:30

## 测试环境
- 服务: http://localhost:8080
- 数据库: MySQL 8.0.32 (port 3307)
- 分支: service-core

## 数据准备
通过 `init_metrics_data.py` 脚本插入了 1000 条模拟指标数据。

## API测试记录

### 1. 服务启动测试 ✅
```bash
curl http://localhost:8080/health
```
结果: PASS
```json
{"code":200,"message":"success","data":{"database":"connected","status":"healthy"}}
```

### 2. TOP基金排名 API ✅
```bash
curl "http://localhost:8080/api/funds/top?sortBy=sharpe&limit=5"
```
结果: PASS
- 返回 TOP 5 基金
- 夏普比率降序排列
- 质量等级正确显示 (S/A/B/C/D)

示例响应:
```json
{
    "code": 200,
    "data": [
        {
            "fundCode": "000922",
            "fundName": "中邮现金驿站货币B",
            "sharpeRatio1y": 2.999,
            "qualityLevel": "S"
        }
    ]
}
```

### 3. 基金对比 API ✅
```bash
curl "http://localhost:8080/api/funds/compare?codes=000001,000922,001101"
```
结果: PASS
- 成功返回3只基金对比数据
- 指标数据完整

### 4. 指标筛选 API ✅
```bash
curl "http://localhost:8080/api/funds/filter?minSharpe=2.0&page=1&size=3"
```
结果: PASS
- 返回夏普比率 >= 2.0 的基金
- 分页正常

### 5. 不同排序字段测试 ✅
```bash
curl "http://localhost:8080/api/funds/top?sortBy=return1y&limit=3"
```
结果: PASS
- sortBy 参数解析正确
- 支持 sharpe/return1y/return3y/maxDrawdown

## 性能测试

### 响应时间
```bash
time curl -s "http://localhost:8080/api/funds/top?limit=10" > /dev/null
```
结果: ~50ms ✅ (< 200ms 要求)

```bash
time curl -s "http://localhost:8080/api/funds/filter?minSharpe=1.0&page=1&size=20" > /dev/null
```
结果: ~80ms ✅ (< 200ms 要求)

## 数据库验证
```sql
SELECT COUNT(*) FROM fund_metrics;
-- 结果: 1000 ✅

SELECT 
    AVG(sharpe_ratio_1y) as avg_sharpe,
    MAX(sharpe_ratio_1y) as max_sharpe,
    COUNT(CASE WHEN sharpe_ratio_1y >= 2 THEN 1 END) as s_count
FROM fund_metrics;
-- 平均: 1.08, 最高: 2.99, S级: 约100只
```

## 测试结果总结

| 检查项 | 状态 | 说明 |
|--------|------|------|
| GET /api/funds/top | ✅ PASS | 返回排名列表 |
| GET /api/funds/compare | ✅ PASS | 返回对比数据 |
| GET /api/funds/filter | ✅ PASS | 返回筛选结果 |
| 响应时间 < 200ms | ✅ PASS | 实际 ~50-80ms |
| 测试日志完整 | ✅ PASS | 已记录 |

## 结论
P2-03 任务完成，所有 API 功能正常，性能达标。

**备注**: 当前使用模拟数据，后续需通过真实净值数据计算指标。
