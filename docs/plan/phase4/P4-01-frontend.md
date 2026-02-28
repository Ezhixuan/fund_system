# P4-01: 前端界面开发 - 执行计划

> 工期：6天 | 依赖：P2-02,P3-03

---

## 技术栈
- Vue 3 + Composition API
- Vite 构建
- Element Plus UI
- ECharts 图表
- Axios HTTP

---

## 页面清单

| 页面 | 功能 | 工期 |
|------|------|------|
| 首页/仪表盘 | 数据概览、快捷入口 | 1天 |
| 基金搜索 | 列表、筛选、拼音搜索 | 1天 |
| 基金详情 | 净值曲线、指标、信号 | 2天 |
| 持仓管理 | 交易记录、收益分析 | 1.5天 |
| 信号历史 | 历史信号、命中率 | 0.5天 |

---

## 核心组件

### 1. 基金搜索组件
```vue
<template>
  <div class="fund-search">
    <!-- 搜索框（支持拼音） -->
    <el-input
      v-model="keyword"
      placeholder="搜索基金代码/名称/拼音"
      @input="debounceSearch"
    />
    
    <!-- 筛选条件 -->
    <div class="filters">
      <el-select v-model="fundType" placeholder="基金类型">
        <el-option label="股票型" value="股票型" />
        <el-option label="混合型" value="混合型" />
      </el-select>
    </div>
    
    <!-- 结果列表 -->
    <el-table :data="fundList">
      <el-table-column prop="fundCode" label="代码" />
      <el-table-column prop="fundName" label="名称" />
      <el-table-column prop="managerName" label="基金经理" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

### 2. 基金详情组件
```vue
<template>
  <div class="fund-detail">
    <!-- 基本信息 -->
    <div class="basic-info">
      <h1>{{ fund.fundName }} ({{ fund.fundCode }})</h1>
      <div class="tags">
        <el-tag>{{ fund.fundType }}</el-tag>
        <el-tag type="success">{{ signal.type }}</el-tag>
      </div>
    </div>
    
    <!-- 净值曲线 -->
    <div class="chart-container">
      <v-chart :option="navChartOption" />
    </div>
    
    <!-- 指标卡片 -->
    <div class="metrics-cards">
      <metric-card
        title="夏普比率"
        :value="metrics.sharpeRatio"
        :trend="metrics.sharpeRatio > 1 ? 'up' : 'down'"
      />
      <metric-card
        title="最大回撤"
        :value="metrics.maxDrawdown + '%'"
      />
    </div>
    
    <!-- 决策信号 -->
    <div class="signal-box" :class="signal.type">
      <h3>{{ signal.text }}</h3>
      <p>{{ signal.reason }}</p>
    </div>
  </div>
</template>
```

---

## 验收清单
- [ ] 响应式布局（PC/平板/手机）
- [ ] 页面加载<3秒
- [ ] 交互流畅
- [ ] 图表正常显示
