# P4-01-06: 前端功能-当日估值

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-01-06 |
| 名称 | 前端功能-当日估值 |
| 状态 | ✅ 已完成 |
| 开始时间 | 2026-03-01 |
| 完成时间 | 2026-03-01 |
| 工时 | 3小时 |

---

## 需求描述
在基金详情页显示实时估值信息，包括涨跌幅、当前净值、交易状态，支持手动刷新。

---

## 实现内容

### 1. 后端API
- `FundEstimateVO.java` - 估值数据对象
- `EstimateService.java` - 估值服务（含缓存）
- `EstimateController.java` - 估值接口
- `FundNavMapper.selectPreviousNav()` - 查询前一交易日净值

### 2. 前端实现
- `api/index.js` - 添加getFundEstimate和refreshFundEstimate接口
- `FundDetail.vue` - 添加估值卡片组件

### 3. 接口列表
```
GET  /api/funds/{code}/estimate        # 获取估值
POST /api/funds/{code}/estimate/refresh # 刷新估值
```

---

## 功能特性
- [x] 显示当日涨跌幅百分比
- [x] 显示当前估值净值
- [x] 显示昨日净值
- [x] 显示交易状态（交易中/已收盘）
- [x] 刷新按钮支持手动刷新
- [x] 后端缓存5分钟

---

## Git提交
```
6d789f4 feat(estimate): 添加基金实时估值功能（后端）
3746fa2 feat(estimate): 添加基金实时估值前端功能
```

---

## 测试日志
详见：[P4-01-06-test-log.md](./P4-01-06-test-log.md)
