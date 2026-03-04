# Task: 修复关注列表UI与选择器Bug

**Created**: 2026-03-04 03:43
**Category**: bugfix
**Status**: Completed
**Priority**: P1

## 任务描述

关注列表页面存在两个问题：
1. **FundSearchSelect 组件Bug**：添加关注时，查询接口返回数据但下拉框空白无法选择
2. **UI样式改造需求**：关注列表页面需要使用 Twitter/X 风格UI

## 执行步骤

### Step 1: 修复 FundSearchSelect 字段映射Bug
- [x] 检查 API 返回数据结构
- [x] 修改 FundSearchSelect.vue，添加字段映射兼容
- [x] 兼容 fundCode/fund_code、fundName/fund_name、fundType/fund_type
- **实际执行**: 修改第56-57行，添加map转换逻辑
- **完成时间**: 2026-03-04 03:44

### Step 2: 引入 Twitter UI CSS 变量
- [x] 复制 twitter-ui.css 到 fund-view/src/styles/
- [x] 在 main.js 中引入全局样式
- **实际执行**: 样式文件已复制并引入
- **完成时间**: 2026-03-04 03:45

### Step 3: 改造关注列表页面样式
- [x] 修改统计卡片样式（圆角、阴影、悬停效果）
- [x] 优化表格样式
- [x] 应用蓝白配色方案
- [x] 添加响应式布局
- **实际执行**: 重写 watchlist/index.vue 样式部分
- **完成时间**: 2026-03-04 03:48

## 实现详情

### 文件修改

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `src/components/FundSearchSelect.vue` | 修改 | 添加字段映射，兼容两种命名规范 |
| `src/main.js` | 修改 | 引入 Twitter UI 样式 |
| `src/views/watchlist/index.vue` | 修改 | 应用 Twitter UI 风格样式 |
| `src/styles/twitter-ui.css` | 新增 | Twitter UI CSS 变量 |

### 代码变更

**FundSearchSelect.vue**:
```javascript
// 兼容后端返回的下划线命名和驼峰命名
options.value = (res.data || []).map(item => ({
  fundCode: item.fundCode || item.fund_code,
  fundName: item.fundName || item.fund_name,
  fundType: item.fundType || item.fund_type
}))
```

**watchlist/index.vue 样式**:
- 使用 CSS 变量：--bg-primary, --bg-secondary, --primary-color 等
- 统计卡片添加悬停动画：transform: translateY(-4px)
- 表格添加圆角边框和悬停效果
- 响应式布局适配移动端

## 测试验证

- [x] 字段映射逻辑正确
- [x] CSS 变量引入成功
- [x] 样式编译通过
- [ ] 前端功能测试（需用户验证）

## 执行结果

- **状态**: ✅ 成功
- **完成时间**: 2026-03-04 03:48
- **耗时**: 5 分钟

## 备注

- 基金搜索选择器现在兼容后端返回的两种字段命名规范
- 关注列表页面已应用 Twitter UI 风格：蓝白配色、圆角卡片、悬停动画
- 建议在浏览器中验证样式效果
