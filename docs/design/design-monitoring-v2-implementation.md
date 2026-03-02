# 监控面板 v2 实现方案

## 承认之前设计的不足

之前的"刷新"功能确实是伪监控：
1. 只是重复调用已有API
2. 无法诊断"为什么获取不到数据"
3. 没有真正的链路追踪能力

## v2 设计核心思想

**不是"刷新"，而是"透视"**

### 1. 链路追踪（ApiTraceService）
记录每次Java -> Python调用的完整过程：
- 请求开始时间、结束时间、耗时
- Python接口URL、HTTP状态码
- Python返回的原始JSON（截断）
- Java处理后的结果
- 错误信息

存储：Redis List，保留最近1000条

### 2. 原始数据查询（绕过Java处理）
直接查询Python服务，不经过Java业务逻辑：
```
GET /api/monitor/raw/python/{fundCode}?type=info
```
这能确认Python端到底返回了什么数据

### 3. 数据对比（Java处理前后）
```
GET /api/monitor/compare/{fundCode}
```
同时展示：
- Python原始返回
- Java处理后返回
对比能看出数据在哪一步丢失了

### 4. 链路统计
统计最近1小时的调用情况：
- 总调用次数
- 成功率
- 平均耗时
- 失败原因分布

## API列表

| 接口 | 说明 |
|------|------|
| GET /api/monitor/traces | 获取链路日志 |
| GET /api/monitor/raw/python/{code}?type=info | 查询Python原始数据 |
| GET /api/monitor/compare/{code} | 对比Java处理前后数据 |
| GET /api/monitor/trace-stats | 链路统计 |

## 前端展示

监控面板应展示：
1. **实时链路流** - 最近调用的时间线
2. **原始数据查看器** - 直接看Python返回
3. **对比视图** - Python原始 vs Java处理后
4. **统计仪表盘** - 成功率、耗时趋势

## 实现状态

- [x] ApiCallLog 实体
- [x] ApiTraceService 链路追踪服务
- [x] CollectClientImplV2 埋点
- [x] MonitorController 查询接口
- [ ] 前端监控面板更新（待完成）

## 价值

这个设计能回答：
1. "为什么这只基金没数据？" -> 看链路日志
2. "Python返回了什么？" -> 查原始数据
3. "Java处理对不对？" -> 对比视图
4. "整体质量如何？" -> 统计仪表盘
