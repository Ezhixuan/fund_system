# P4-02: 数据监控告警 - 执行计划

> 工期：3天 | 依赖：P1-04

---

## 监控面板

### 1. 数据新鲜度监控
```
┌─────────────────────────────────────┐
│        数据监控面板                  │
├─────────────────────────────────────┤
│  fund_nav: 最新 2026-02-28 ✓       │
│  fund_metrics: 最新 2026-02-28 ✓   │
│  fund_score: 最新 2026-02-28 ✓     │
├─────────────────────────────────────┤
│  今日采集: 15000条 | 成功率: 98%    │
│  校验通过: 14700条 | 失败: 300条    │
└─────────────────────────────────────┘
```

### 2. API监控
```
┌─────────────────────────────────────┐
│        API性能监控                   │
├─────────────────────────────────────┤
│  /api/funds/search: P99 150ms ✓    │
│  /api/funds/{code}/metrics: 80ms ✓ │
│  缓存命中率: 85%                     │
└─────────────────────────────────────┘
```

---

## 告警规则

| 条件 | 级别 | 通知方式 |
|------|------|----------|
| 采集失败率>5% | 严重 | 钉钉+邮件 |
| 数据延迟>1天 | 严重 | 钉钉+邮件 |
| API P99>500ms | 警告 | 钉钉 |
| 缓存命中率<50% | 警告 | 钉钉 |

---

## 实现

```python
# monitor/alert_scheduler.py
from apscheduler.schedulers.background import BackgroundScheduler

scheduler = BackgroundScheduler()

# 每30分钟检查一次
@scheduler.scheduled_job('interval', minutes=30)
def check_data_freshness():
    # 检查数据延迟
    pass

@scheduler.scheduled_job('interval', minutes=30)
def check_api_performance():
    # 检查API性能
    pass

scheduler.start()
```

---

## 验收清单
- [ ] 监控面板可查看
- [ ] 告警规则生效
- [ ] 钉钉通知正常
- [ ] 数据质量报告
