# P4-01-07: 前端功能-持仓编辑

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P4-01-07 |
| 名称 | 前端功能-持仓编辑 |
| 状态 | ✅ 已完成 |
| 开始时间 | 2026-03-01 |
| 完成时间 | 2026-03-01 |
| 工时 | 4小时 |

---

## 需求描述
支持修改持仓的份额和成本价，支持删除持仓（清仓）。

---

## 实现内容

### 1. 后端API
- `HoldingUpdateRequest.java` - 更新请求对象
- `PortfolioService.updateHolding()` - 更新持仓
- `PortfolioService.adjustShares()` - 调整份额
- `PortfolioService.adjustCost()` - 调整成本
- `PortfolioService.deleteHolding()` - 删除持仓
- `PortfolioController` - 添加PUT/DELETE接口

### 2. 前端实现
- `api/index.js` - 添加updateHolding和deleteHolding接口
- `Portfolio.vue` - 添加编辑弹窗和删除确认

### 3. 接口列表
```
PUT    /api/portfolio/holdings/{fundCode}  # 更新持仓
DELETE /api/portfolio/holdings/{fundCode}  # 删除持仓
```

---

## 功能特性
- [x] 修改持仓份额
- [x] 修改持仓成本价
- [x] 删除持仓（自动清仓）
- [x] 删除确认对话框

---

## Git提交
```
8b71e19 feat(portfolio): 添加持仓编辑和删除功能（后端）
71448a6 feat(portfolio): 添加持仓编辑和删除前端功能
```

---

## 测试日志
详见：[P4-01-07-test-log.md](./P4-01-07-test-log.md)
