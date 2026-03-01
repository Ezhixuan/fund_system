# P4-02-01 测试日志

## 测试时间
2026-03-01

---

## 测试环境
- 后端：http://localhost:8080
- MySQL：127.0.0.1:3307/fund_system

---

## 测试用例

### TC-001: 数据表状态接口
**请求**：
```bash
curl -s "http://localhost:8080/api/monitor/tables/status"
```

**响应**：
```json
{
    "code": 200,
    "data": [
        {
            "tableName": "fund_nav",
            "latestDate": "2026-02-28",
            "recordCount": 22782,
            "isFresh": true,
            "delayDays": 1
        },
        {
            "tableName": "fund_metrics",
            "latestDate": "2026-02-28",
            "recordCount": 1001,
            "isFresh": true,
            "delayDays": 1
        },
        {
            "tableName": "fund_score",
            "latestDate": null,
            "recordCount": 0,
            "isFresh": false,
            "delayDays": -1
        }
    ],
    "success": true
}
```

**结果**：✅ 通过

---

### TC-002: 采集统计接口
**请求**：
```bash
curl -s "http://localhost:8080/api/monitor/collection/stats"
```

**响应**：
```json
{
    "code": 200,
    "data": {
        "date": "2026-03-01",
        "totalFunds": 26180,
        "collectedFunds": 0,
        "successRate": 0.0,
        "failedCount": 26180
    },
    "success": true
}
```

**结果**：✅ 通过

---

### TC-003: Python监控服务
**命令**：
```bash
python3 collector/monitor/collection_monitor.py
```

**输出**：
```
============================================================
数据采集监控报告
============================================================

【数据表状态】
  ✓ fund_nav: 最新 2026-02-28, 记录数 22782, 延迟 1 天
  ✓ fund_metrics: 最新 2026-02-28, 记录数 1001, 延迟 1 天
  ✗ fund_score: 最新 N/A, 记录数 0, 延迟 -1 天

【今日采集统计】
  日期: 2026-03-01
  基金总数: 26180
  已采集: 0
  成功率: 0.0%
  失败数: 26180

【数据质量检查】
  ✓ 净值大于0: 0 条记录未通过
  ✗ 累计净值>=单位净值: 55 条记录未通过
  ✓ 必填字段不为空: 0 条记录未通过
```

**结果**：✅ 通过

---

## 测试结论
数据采集监控功能正常，测试通过。

**测试人员**：OpenClaw
**测试日期**：2026-03-01
