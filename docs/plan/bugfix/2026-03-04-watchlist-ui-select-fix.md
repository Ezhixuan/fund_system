# 计划: 关注列表页面UI改造与选择器Bug修复

**Created**: 2026-03-04 03:43
**Category**: bugfix
**Status**: Draft
**Priority**: P1

## 背景

关注列表页面存在两个问题需要修复：

1. **FundSearchSelect 组件Bug**：添加关注时，查询接口返回数据但下拉框空白无法选择，控制台报错 `Invalid prop: type check failed for prop "value"`。原因是API返回字段名为下划线命名（`fund_code`），而组件使用驼峰命名（`fundCode`）。

2. **UI样式改造需求**：关注列表页面需要使用 Twitter/X 风格UI（蓝白配色、圆角卡片、悬停动画效果）。

## 目标

1. 修复 FundSearchSelect.vue 字段映射问题，兼容两种命名规范
2. 按照 Twitter UI Vue 规范改造关注列表页面样式
3. 确保基金搜索选择功能正常工作
4. 保持响应式布局适配

## 执行步骤

### Step 1: 修复 FundSearchSelect 字段映射Bug
- [ ] 检查 API 返回数据结构
- [ ] 修改 FundSearchSelect.vue 第56-57行，添加字段映射
- [ ] 兼容 `fundCode/fund_code`、`fundName/fund_name`、`fundType/fund_type`
- [ ] 测试搜索选择功能
- **预期产出**: FundSearchSelect 组件可正常显示和选择基金
- **截止时间**: 2026-03-04 03:45

### Step 2: 引入 Twitter UI CSS 变量
- [ ] 复制 twitter-ui.css 到 fund-view/src/styles/
- [ ] 在 main.js 中引入全局样式
- [ ] 验证CSS变量生效
- **预期产出**: 全局可用 Twitter UI CSS 变量
- **截止时间**: 2026-03-04 03:47

### Step 3: 改造关注列表页面样式
- [ ] 修改统计卡片样式（圆角、阴影、悬停效果）
- [ ] 改造按钮样式（主按钮、轮廓按钮）
- [ ] 优化表格样式或改为卡片式布局
- [ ] 应用蓝白配色方案
- **预期产出**: 关注列表页面符合 Twitter UI 风格
- **截止时间**: 2026-03-04 03:52

### Step 4: 测试验证
- [ ] 测试基金搜索选择功能
- [ ] 测试添加关注流程
- [ ] 测试响应式布局（PC/平板/手机）
- [ ] 测试悬停动画效果
- **预期产出**: 所有功能正常，UI效果符合预期
- **截止时间**: 2026-03-04 03:55

## 依赖关系

```
Step 1 (Bug修复) → Step 2 (引入样式) → Step 3 (页面改造)
     ↓                                    ↓
Step 4 (测试验证) ←──────────────────────┘
```

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| API字段名不一致 | Med | 同时兼容驼峰和下划线命名 |
| 样式冲突 | Low | 使用CSS变量，避免全局污染 |
| 响应式问题 | Low | 使用Twitter UI提供的响应式断点 |

## 相关文档

- Issue: [关注列表页面UI改造与选择器Bug修复](../../issues/2026-03-04-0342-watchlist-ui-and-select-bug.md)
- UI规范: `docs/skill/twitter-ui-vue/SKILL.md`
- 样式文件: `docs/skill/twitter-ui-vue/twitter-ui.css`
- 目标文件:
  - `fund-view/src/components/FundSearchSelect.vue`
  - `fund-view/src/views/watchlist/index.vue`

## 备注

- Step 1 优先修复Bug，确保功能可用
- Step 2-3 可并行进行样式改造
- 注意保持Element Plus组件的基本功能不变，仅修改样式
