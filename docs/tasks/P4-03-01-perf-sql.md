# P4-03-01: 性能优化-SQL查询优化

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-03-01 |
| 名称 | 性能优化-SQL查询优化 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 1天 |
| 依赖 | 所有 |

---

## 需求描述
优化慢查询，提升数据库性能。

---

## 实现步骤

### 1. 慢查询分析
使用MySQL慢查询日志，找出慢查询：
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
```

### 2. 索引优化
检查并添加必要索引：
```sql
-- fund_nav表
ALTER TABLE fund_nav ADD INDEX idx_fund_date (fund_code, nav_date DESC);

-- fund_metrics表  
ALTER TABLE fund_metrics ADD INDEX idx_fund_calc (fund_code, calc_date DESC);

-- portfolio_trade表
ALTER TABLE portfolio_trade ADD INDEX idx_fund_date (fund_code, trade_date DESC);
```

### 3. SQL优化
优化查询语句：
- 避免SELECT *
- 添加LIMIT限制
- 优化JOIN查询

---

## 验收标准
- [ ] 慢查询数量减少80%
- [ ] 核心API响应时间<200ms
- [ ] 所有查询使用索引

---

## 测试计划
测试日志将记录在：[P4-03-01-test-log.md](./P4-03-01-test-log.md)
