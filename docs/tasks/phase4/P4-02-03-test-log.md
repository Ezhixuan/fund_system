# P4-02-03 测试日志

## 测试时间
2026-03-01

---

## 测试环境
- 后端：http://localhost:8080
- Python：3.11

---

## 测试用例

### TC-001: Python告警检查
**命令**：
```bash
cd collector && python3 monitor/alert_checker.py
```

**输出**：
```
============================================================
告警检查
============================================================

🔴 [CRITICAL] 2026-03-01 15:29:22
   规则: collection_failure_rate
   消息: 采集成功率过低: 0.0% (< 95.0%)
   数据: {'date': '2026-03-01', 'total_funds': 26180, 'collected_funds': 0, 'success_rate': 0.0, 'failed_count': 26180}


共发现 1 个告警
严重: 1, 警告: 0, 信息: 0
============================================================
```

**结果**：✅ 通过
- 正确检测到采集失败率告警
- 告警级别正确（critical）
- 告警数据完整

---

### TC-002: 告警规则接口
**请求**：
```bash
curl -s "http://localhost:8080/api/monitor/alerts/rules"
```

**响应**：
```json
{
    "code": 200,
    "data": [
        {
            "condition": "success_rate < 95%",
            "level": "critical",
            "name": "collection_failure_rate",
            "description": "采集成功率低于95%"
        },
        {
            "condition": "delay_days > 1",
            "level": "critical",
            "name": "data_delay",
            "description": "数据延迟超过1天"
        },
        {
            "condition": "p99 > 500ms",
            "level": "warning",
            "name": "api_slow",
            "description": "API响应时间超过500ms"
        },
        {
            "condition": "error_rate > 5%",
            "level": "warning",
            "name": "api_error_rate",
            "description": "API错误率超过5%"
        }
    ],
    "success": true
}
```

**结果**：✅ 通过
- 返回4条告警规则
- 规则信息完整

---

### TC-003: 当前告警接口
**请求**：
```bash
curl -s "http://localhost:8080/api/monitor/alerts/current"
```

**响应**：
```json
{
    "code": 200,
    "data": [
        {
            "data": {
                "totalFunds": 26180,
                "successRate": 0.0,
                "collectedFunds": 0,
                "date": "2026-03-01"
            },
            "level": "critical",
            "ruleName": "collection_failure_rate",
            "message": "采集成功率过低: 0.0% (< 95%)",
            "timestamp": "2026-03-01T07:30:47.581660Z"
        }
    ],
    "success": true
}
```

**结果**：✅ 通过
- 正确返回当前活跃告警
- 告警数据完整

---

## 测试结论
告警通知机制功能正常，测试通过。

**测试人员**：OpenClaw
**测试日期**：2026-03-01
