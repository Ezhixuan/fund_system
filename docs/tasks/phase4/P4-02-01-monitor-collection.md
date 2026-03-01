# P4-02-01: 监控告警-数据采集监控

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-02-01 |
| 名称 | 监控告警-数据采集监控 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 1天 |
| 依赖 | P1-04 |

---

## 需求描述
实现数据采集状态监控，包括：
1. 各表数据新鲜度监控
2. 每日采集成功率统计
3. 数据校验通过率

---

## 实现步骤

### 1. 创建监控服务
**文件**：`collector/monitor/collection_monitor.py`

功能：
- 查询各表最新数据日期
- 统计今日采集记录数
- 计算采集成功率

### 2. 创建监控API
**文件**：`fund-service/src/main/java/com/fund/controller/MonitorController.java`

接口：
```
GET /api/monitor/collection/status    # 采集状态
GET /api/monitor/collection/stats     # 采集统计
```

### 3. 前端监控面板
**文件**：`fund-view/src/views/Monitor.vue`

展示：
- 各表最新数据时间
- 今日采集成功率
- 异常数据提示

---

## 验收标准
- [ ] 能正确显示各表数据新鲜度
- [ ] 采集成功率统计准确
- [ ] 数据延迟>1天有警告提示

---

## 测试计划
测试日志将记录在：[P4-02-01-test-log.md](./P4-02-01-test-log.md)
