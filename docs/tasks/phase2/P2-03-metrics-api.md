# Task: P2-03-metrics-api

## 任务信息
- **任务ID**: P2-03
- **任务名称**: 指标计算与排名API
- **所属阶段**: Phase 2 后端核心层
- **计划工期**: 4天
- **状态**: 🔄 进行中
- **工作分支**: service-core
- **前置依赖**: P2-02 ✅

## 需求分析
基于 plan/P2-03-metrics-api.md，本任务需要实现：
1. TOP基金排名API（按夏普比率、收益率等排序）
2. 基金指标对比API
3. 基金筛选API（按多维度指标筛选）

## API设计

### 1. TOP基金排名
```
GET /api/funds/top?sortBy=sharpe&fundType=混合型&limit=10
```
参数:
- `sortBy`: 排序字段 (sharpe/return1y/return3y/maxDrawdown)
- `fundType`: 基金类型筛选（可选）
- `limit`: 返回数量（默认10，最大50）

### 2. 基金指标对比
```
GET /api/funds/compare?codes=000001,000002,000003
```
参数:
- `codes`: 基金代码列表，逗号分隔（最多5个）

### 3. 基金筛选
```
GET /api/funds/filter?minSharpe=1.5&maxDrawdown=20&fundType=混合型
```
参数:
- `minSharpe`: 最小夏普比率
- `maxDrawdown`: 最大回撤上限
- `fundType`: 基金类型
- `page/size`: 分页

## 执行计划

### Day 1-2: 排名与对比API ✅
- [x] MetricsService 扩展接口
- [x] TOP排名实现
- [x] 指标对比实现
- [x] FundController 扩展实现

### Day 3-4: 测试与优化 ⚠️
- [x] 接口测试（数据为空）
- [x] 测试日志记录
- [ ] 性能测试（等待数据补充）

## 验收标准
- [x] GET /api/funds/top 接口实现
- [x] GET /api/funds/compare 接口实现
- [x] GET /api/funds/filter 接口实现
- [ ] 返回有效数据（依赖 fund_metrics 数据补充）
- [ ] 响应时间 < 200ms（待验证）
- [x] 测试日志完整

## 测试结果
**状态**: 代码实现完成，数据依赖待补充

详见: [test-log-p2-03.md](./test-log-p2-03.md)

## 问题记录
**fund_metrics 表为空** - Phase 1 数据采集中未完成指标计算模块

**影响**: 
- API 返回格式正确但数据为空
- 无法验证实际业务效果

**解决方案**:
1. 在 Phase 3 指标引擎完成后补充数据
2. 或使用 Python 脚本临时计算并插入测试数据

## 风险提醒
- fund_metrics 表数据为空，API 返回空数组
- 需要在 Phase 3 完成后重新测试验证
- 排序字段需要验证有效性
