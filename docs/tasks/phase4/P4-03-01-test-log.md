# P4-03-01 测试日志

## 测试时间
2026-03-01

---

## 测试环境
- 后端：http://localhost:8080
- 数据库：MySQL 8.0

---

## 测试用例

### TC-001: API性能测试
**步骤**：
1. 清除性能统计
2. 触发5次 /api/funds/011452/nav 请求
3. 触发5次 /api/funds/011452/metrics 请求
4. 查看性能统计

**请求**：
```bash
# 清除统计
curl -s "http://localhost:8080/api/monitor/api/performance/clear"

# 触发请求
for i in {1..5}; do
    curl -s "http://localhost:8080/api/funds/011452/nav"
    curl -s "http://localhost:8080/api/funds/011452/metrics"
done

# 查看统计
curl -s "http://localhost:8080/api/monitor/api/performance"
```

**响应**：
```json
{
    "code": 200,
    "data": {
        "apis": {
            "GET /api/funds/011452/nav": {
                "p99": 173,
                "minResponseTime": 3,
                "maxResponseTime": 173,
                "avgResponseTime": 37,
                "totalRequests": 5,
                "errorRate": 0.0,
                "errorRequests": 0,
                "p95": 173
            },
            "GET /api/funds/011452/metrics": {
                "p99": 50,
                "minResponseTime": 2,
                "maxResponseTime": 50,
                "avgResponseTime": 12,
                "totalRequests": 5,
                "errorRate": 0.0,
                "errorRequests": 0,
                "p95": 50
            }
        },
        "overall": {
            "maxResponseTime": 173,
            "avgResponseTime": 25,
            "totalRequests": 10,
            "errorRate": 0.0,
            "errorRequests": 0
        }
    },
    "success": true
}
```

**结果**：✅ 通过
- /nav API平均响应时间：37ms (< 200ms)
- /metrics API平均响应时间：12ms (< 200ms)
- 所有请求成功率100%

---

### TC-002: SQL优化验证
**验证**：检查Mapper文件

**优化内容**：
- FundNavMapper: SELECT * 改为 SELECT fund_code, nav_date, unit_nav, accum_nav, daily_change
- FundMetricsMapper: SELECT * 改为指定字段查询
- FundMetricsMapper.xml: SELECT * 改为指定字段查询

**结果**：✅ 通过

---

## 测试结论
SQL查询优化完成，API性能满足要求。

**测试人员**：OpenClaw
**测试日期**：2026-03-01
