# 监控面板设计 v2 - 链路追踪与数据质量监控

## 问题分析

### 之前方案的问题
1. **伪监控**: "刷新"按钮只是重复调用已有API，无法解决根本问题
2. **无链路追踪**: 无法看到 Java -> Python 的调用过程
3. **无原始数据**: 无法确认Python返回了什么数据
4. **无质量分析**: 无法系统性分析哪些基金缺数据、缺什么

### 真正需要的监控能力

## 设计方案

### 1. API调用链路追踪

**目标**: 记录每一次 Java API 调用 Python 服务的完整过程

**数据模型**:
```java
@ApiCallLog
- requestId: 请求唯一ID
- fundCode: 基金代码
- apiType: 调用类型 (info/metrics/nav/estimate)
- startTime: 开始时间
- endTime: 结束时间
- duration: 耗时(ms)
- status: 状态 (success/timeout/error)
- javaResponse: Java返回给前端的数据摘要
- pythonRequest: 请求Python的参数
- pythonResponse: Python返回的原始数据
- errorMessage: 错误信息
```

**存储**: Redis + 定时持久化到MySQL

### 2. Python原始数据展示

**目标**: 直接展示Python采集服务返回的原始数据

**数据来源**:
- 不是通过Java转述，而是直接查询Python服务
- 展示Python能获取到的原始数据
- 与Java处理后的数据对比

**展示内容**:
- 原始JSON数据结构
- 数据字段完整性检查
- 数据新鲜度（采集时间）

### 3. 数据质量大盘

**目标**: 系统性分析数据覆盖情况

**指标**:
- 基金总数 vs 有完整数据的基金数
- 各字段缺失率统计
- 按基金类型分析数据质量
- 数据采集成功率趋势

### 4. 实时告警

**触发条件**:
- Python服务连续5分钟无响应
- 某只基金连续3次采集失败
- 数据完整度低于50%

## 实现规划

### Phase 1: 链路追踪埋点
- 在CollectClientImpl中记录每次调用
- 存储到Redis，保留最近1000条

### Phase 2: 原始数据查询
- 直接对接Python服务API
- 绕过Java业务逻辑

### Phase 3: 质量分析
- 定时扫描数据库统计缺失情况
- 生成数据质量报告

### Phase 4: 监控面板
- 可视化展示链路、原始数据、质量指标
