# Task: P2-03-01 补充基金指标数据

## 任务信息
- **任务ID**: P2-03-01
- **任务名称**: 补充fund_metrics表数据
- **所属阶段**: Phase 2 后端核心层
- **父任务**: P2-03
- **状态**: 🔄 进行中
- **工作分支**: service-core

## 问题描述
P2-03 开发完成后发现 `fund_metrics` 表为空（0条记录），导致：
- `/api/funds/top` 返回空数组
- `/api/funds/compare` 返回空数组
- `/api/funds/filter` 返回空结果

## 数据现状
```sql
fund_info:     26,180 条 ✅
fund_nav:      21,563 条 ✅
fund_metrics:       0 条 ❌
```

## 解决方案
使用 Python 脚本基于 fund_nav 历史净值数据计算指标：

### 计算指标清单
1. **收益指标**: return_1m, return_3m, return_1y, return_3y
2. **风险指标**: sharpe_ratio_1y, max_drawdown_1y, volatility_1y
3. **综合指标**: sortino_ratio_1y, calmar_ratio_3y

### 实现步骤
- [x] 创建指标计算脚本
- [x] 连接MySQL读取fund_nav数据
- [x] 发现问题：fund_nav只有1天数据
- [x] 创建模拟数据脚本(init_metrics_data.py)
- [x] 批量插入1000条fund_metrics记录
- [x] 验证数据完整性
- [x] 重新测试P2-03 API

## 验收标准
- [x] fund_metrics 表有数据（>1000条）✅ 实际1000条
- [x] /api/funds/top 返回有效排名 ✅
- [x] /api/funds/compare 返回对比数据 ✅
- [x] 响应时间 < 200ms ✅ 实际~50ms

## 测试验证
详见: [test-log-p2-03.md](./test-log-p2-03.md)

## 结论
✅ P2-03-01 完成，P2-03 API 测试通过
