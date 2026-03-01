# P4-02-03: 监控告警-告警通知机制

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-02-03 |
| 名称 | 监控告警-告警通知机制 |
| 状态 | ✅ 已完成 |
| 开始时间 | 2026-03-01 |
| 完成时间 | 2026-03-01 |
| 计划工期 | 1天 |
| 实际工时 | 2小时 |
| 依赖 | P4-02-01, P4-02-02 |

---

## 需求描述
实现自动告警通知机制：
1. 配置告警规则
2. 支持钉钉/邮件通知（预留接口）
3. 定时检查告警条件

---

## 实现内容

### 1. Python告警检查器
**文件**：`collector/monitor/alert_checker.py`

功能：
- 检查采集成功率
- 检查数据新鲜度
- 检查API性能
- 触发告警通知

### 2. 告警调度器
**文件**：`collector/monitor/alert_scheduler.py`

功能：
- 定时执行告警检查
- 支持命令行参数
- 信号处理（优雅退出）

### 3. Java告警API
**文件**：
- `fund-service/src/main/java/com/fund/dto/AlertDTO.java`
- `fund-service/src/main/java/com/fund/controller/MonitorController.java`

接口：
```
GET /api/monitor/alerts/rules    # 获取告警规则
GET /api/monitor/alerts/current  # 获取当前告警
```

### 4. 告警规则

| 规则名 | 级别 | 条件 | 描述 |
|--------|------|------|------|
| collection_failure_rate | critical | success_rate < 95% | 采集成功率低于95% |
| data_delay | critical | delay_days > 1 | 数据延迟超过1天 |
| api_slow | warning | p99 > 500ms | API响应时间超过500ms |
| api_error_rate | warning | error_rate > 5% | API错误率超过5% |

---

## 使用方法

### Python告警检查
```bash
# 执行一次检查
cd collector && python3 monitor/alert_checker.py

# 启动定时调度（每30分钟）
cd collector && python3 monitor/alert_scheduler.py

# 自定义间隔（每10分钟）
cd collector && python3 monitor/alert_scheduler.py --interval 10
```

---

## 验收标准
- [x] 告警规则可配置
- [x] 告警检查定时执行
- [x] 告警频率控制（调度器间隔配置）

---

## Git提交
```
e14bdce feat(monitor): 添加告警通知机制
```

---

## 测试日志
详见：[P4-02-03-test-log.md](./P4-02-03-test-log.md)
