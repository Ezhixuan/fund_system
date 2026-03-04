# Issue: 关注列表页面样式改造与基金选择器Bug修复

**Created**: 2026-03-04 03:42
**Status**: Fixed
**Priority**: High
**Fixed At**: 2026-03-04 03:48

## Problem Description

### 问题1：样式改造需求
关注列表页面需要使用 `docs/skill/twitter-ui-vue/` 中定义的 Twitter/X 风格UI：
- 蓝白配色（主色调 #00acee）
- 圆角卡片设计（16px-9999px）
- 悬停动画效果
- 响应式布局

### 问题2：FundSearchSelect 组件Bug
添加关注时，查询接口返回了数据，但下拉框空白无法选择，控制台报错：
```
FundSearchSelect.vue:57 Invalid prop: type check failed for prop "value".
Expected String | Number | Boolean | Object, got Undefined
```

## Environment

- Project: fund-system/fund-view
- Vue Version: 3.x
- UI Library: Element Plus
- 相关文件：
  - `src/views/watchlist/index.vue` - 关注列表页面
  - `src/components/FundSearchSelect.vue` - 基金搜索选择器
  - `docs/skill/twitter-ui-vue/SKILL.md` - UI规范文档

## Analysis

### Bug 根因分析

查看 `FundSearchSelect.vue` 第 13-24 行：
```vue
<el-option
  v-for="item in options"
  :key="item.fundCode"
  :label="`${item.fundName} (${item.fundCode})`"
  :value="item.fundCode"  <!-- 这里报错 -->
>
```

API返回的数据中，基金代码字段可能是 `fund_code`（下划线命名）而非 `fundCode`（驼峰命名），导致 `item.fundCode` 为 `undefined`。

检查 API 响应数据结构，需要确认：
1. 后端返回的字段名是 `fundCode` 还是 `fund_code`
2. 前端是否正确处理了字段映射

## Solution

### 方案1：修复 FundSearchSelect 字段映射（推荐）

在 `FundSearchSelect.vue` 中处理字段映射，兼容两种命名规范：

```javascript
// 第56-57行修改
const res = await searchFund(query)
options.value = (res.data || []).map(item => ({
  fundCode: item.fundCode || item.fund_code,
  fundName: item.fundName || item.fund_name,
  fundType: item.fundType || item.fund_type
}))
```

### 方案2：关注列表页面样式改造

按照 `docs/skill/twitter-ui-vue/SKILL.md` 规范改造：

1. **引入CSS变量**（在全局或组件内）
2. **统计卡片改造**：使用 `.stat-card` 样式，大圆角、悬停阴影
3. **表格改造**：使用卡片式布局或优化表格样式
4. **按钮改造**：使用 Twitter 风格的 `.btn-primary` 和 `.btn-outline`

关键样式应用：
```scss
// 统计卡片
.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  .stat-card {
    background: var(--bg-primary);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    border: 1px solid var(--border-color);
    padding: 24px;
    text-align: center;
    transition: var(--transition);

    &:hover {
      box-shadow: var(--shadow-md);
      transform: translateY(-4px);
    }
  }
}
```

## Action Items

- [ ] 修复 FundSearchSelect.vue 字段映射问题
- [ ] 引入 Twitter UI CSS 变量
- [ ] 改造关注列表页面样式
- [ ] 测试基金搜索选择功能
- [ ] 测试响应式布局

## Related

- UI规范：`docs/skill/twitter-ui-vue/SKILL.md`
- 样式文件：`docs/skill/twitter-ui-vue/twitter-ui.css`
- 相关组件：
  - `src/views/watchlist/index.vue`
  - `src/components/FundSearchSelect.vue`
