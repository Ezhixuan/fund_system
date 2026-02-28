# 设计方案补充说明

## 1. 持仓编辑功能增强

### 需求更新
支持修改：**份额** 和 **成本价**

### 实现方式
```
编辑持仓弹窗:
┌─────────────────────────────────────────┐
│ 编辑持仓                                  │
├─────────────────────────────────────────┤
│ 基金: 华夏成长证券投资基金                 │
│                                         │
│ 当前份额: 1500                           │
│ 修改份额: [  2000  ] ← 不修改可留空       │
│                                         │
│ 当前成本: 1.228元                         │
│ 修改成本: [  1.250  ] ← 不修改可留空      │
│                                         │
│ 备注: [________________]                 │
│                                         │
│ [取消]                  [保存]           │
└─────────────────────────────────────────┘
```

### 后端逻辑
```java
@PutMapping("/portfolio/holdings/{fundCode}")
public ApiResponse<Void> updateHolding(
        @PathVariable String fundCode,
        @RequestBody HoldingUpdateRequest request) {
    
    // 修改份额
    if (request.getTotalShares() != null) {
        adjustShares(fundCode, request.getTotalShares());
    }
    
    // 修改成本价 - 通过添加一笔"成本调整"交易
    if (request.getAvgCost() != null) {
        adjustCost(fundCode, request.getAvgCost());
    }
    
    return ApiResponse.success();
}
```

---

## 2. 当日估值概念澄清

### 你的理解是对的 ✅
当日估值 = **预估涨跌幅** (不是预估净值)

### 两种展示方式对比

| 方式 | 展示内容 | 示例 | 适用场景 |
|------|---------|------|---------|
| **预估涨跌幅** | 相比昨日净值的涨跌% | +1.25% / -0.8% | ✅ 推荐，直观 |
| **预估净值** | 基于持仓股票实时计算的净值 | 2.1567元 | 需要复杂计算 |

### 推荐方案：预估涨跌幅

**数据来源**:
```python
# akshare 获取预估涨跌幅
df = ak.fund_value_estimate_em(symbol='011452')
# 返回: 基金名称, 估算日期, 估算值, 估算增长率%
```

**前端展示**:
```
华夏成长 (000001)
┌─────────────────────────────────┐
│ 实时估值        当日涨跌          │
│                                 │
│   --           +1.25%           │
│  待收盘         🔴 上涨          │
│                                 │
│ [🔄 刷新]  更新时间: 14:32      │
│                                 │
│ 昨日净值: 1.234                 │
└─────────────────────────────────┘
```

**关键点**:
- 9:30-15:00 显示实时估值涨跌幅
- 15:00后显示当日实际涨跌幅（已收盘）
- 非工作日显示最近一个交易日的涨跌幅

---

## 3. 现有基金类型分布

### 指什么？
统计数据库里已有的基金类型，看看有哪些分类

### 为什么需要？
设计"板块划分"时，需要根据现有数据来决定如何分组

### 当前类型分布
让我查一下：

```sql
SELECT fund_type, COUNT(*) as count 
FROM fund_info 
WHERE fund_type IS NOT NULL 
GROUP BY fund_type 
ORDER BY count DESC;
```

### 预期结果
| 基金类型 | 数量 | 建议板块归属 |
|---------|------|-------------|
| 混合型-偏股 | 8,000+ | 权益类-混合 |
| 股票型-普通 | 3,000+ | 权益类-股票 |
| 债券型-纯债 | 2,000+ | 固收类-债券 |
| 指数型-股票 | 1,500+ | 权益类-指数 |
| 货币型 | 1,000+ | 现金类-货币 |
| QDII | 500+ | 海外类-QDII |

**结论**: 板块划分可以基于这些现有类型做聚合

---

## 4. 图标替换方案

### 问题
Emoji 图标在不同系统显示不一致，且不够专业

### 推荐方案：SVG 图标库

#### 方案1: Element Plus 图标（推荐）
```vue
<!-- 安装 -->
npm install @element-plus/icons-vue

<!-- 使用 -->
<import { TrendCharts, Money, DataLine, Wallet } from '@element-plus/icons-vue'/>

<!-- 替换示例 -->
<!-- 原: 📈 -->  <TrendCharts />  趋势/收益
<!-- 原: 💰 -->  <Money />       金额/财富
<!-- 原: 📊 -->  <DataLine />    数据/图表
<!-- 原: 💼 -->  <Wallet />      持仓/钱包
```

#### 方案2: Iconify（轻量级）
```vue
<!-- 使用 CDN -->
<script src="https://code.iconify.design/3/3.1.0/iconify.min.js"></script>

<!-- 图标 -->
<span class="iconify" data-icon="mdi:trending-up"></span>     上涨
<span class="iconify" data-icon="mdi:trending-down"></span>   下跌
<span class="iconify" data-icon="mdi:wallet-outline"></span>  钱包
<span class="iconify" data-icon="mdi:chart-line"></span>     图表
```

### 图标替换映射表

| 场景 | 原 Emoji | 替换图标 | 图标库 |
|------|---------|---------|--------|
| 收益/上涨 | 📈 | <TrendCharts /> | Element Plus |
| 收益/下跌 | 📉 | <TrendCharts style="transform: rotate(180deg)" /> | Element Plus |
| 金额/现金 | 💰 | <Money /> | Element Plus |
| 数据/统计 | 📊 | <DataLine /> | Element Plus |
| 持仓/钱包 | 💼 | <Wallet /> | Element Plus |
| 搜索 | 🔍 | <Search /> | Element Plus |
| 删除 | 🗑️ | <Delete /> | Element Plus |
| 编辑 | ✏️ | <Edit /> | Element Plus |
| 添加 | ➕ | <Plus /> | Element Plus |
| 返回 | ← | <ArrowLeft /> | Element Plus |
| 刷新 | 🔄 | <Refresh /> | Element Plus |
| 金牌排名 | 🥇 | <Medal /> + 颜色 | Element Plus |
| 银牌排名 | 🥈 | <Medal /> + 颜色 | Element Plus |
| 铜牌排名 | 🥉 | <Medal /> + 颜色 | Element Plus |
| 买入信号 | 📈 | <CircleCheckFilled /> 绿色 | Element Plus |
| 卖出信号 | 📉 | <CircleCloseFilled /> 红色 | Element Plus |
| 持有信号 | ⏸️ | <WarningFilled /> 黄色 | Element Plus |

### 实施步骤

1. **安装图标库**
   ```bash
   cd fund-view
   npm install @element-plus/icons-vue
   ```

2. **全局注册**
   ```javascript
   // main.js
   import * as ElementPlusIconsVue from '@element-plus/icons-vue'
   
   for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
     app.component(key, component)
   }
   ```

3. **替换所有 Emoji**
   批量替换所有视图文件中的 Emoji 为对应图标组件

---

## 实施顺序更新

### 立即修复（今天）
1. ✅ 翻页问题
2. ✅ 类型搜索

### 高优先级（本周）
3. **图标替换** - 影响整体视觉效果
4. **当日估值** - 核心功能
5. **持仓编辑增强** - 支持份额+成本修改

### 中优先级（下周）
6. 方案A净值确认
7. 板块划分（先调研类型分布）

---

## 需要确认

1. **图标方案**: 使用 Element Plus 图标可以吗？
2. **当日估值**: 是否只需要涨跌幅%展示，不需要预估净值？
3. **持仓编辑**: 修改成本价时，是否需要记录调整历史？
