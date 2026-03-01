# P5-05: 持仓页面集成

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-05 |
| 名称 | 持仓页面集成 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 2天 |
| 依赖 | P5-04（分时图与手动刷新）|

---

## 需求描述

将实时估值集成到持仓页面：
1. 持仓列表显示每只基金的实时估值/涨跌幅
2. 根据交易时间/非交易时间显示不同数据
3. 预估收益计算（基于实时估值）
4. 整体持仓收益汇总

---

## 实现步骤

### Day 1: 持仓列表实时估值显示
- [ ] 后端 API
  - [ ] GET /api/portfolio/holdings-with-estimate
  - [ ] 返回持仓+实时估值数据
- [ ] 前端列表组件
  - [ ] 显示实时估值/涨跌幅
  - [ ] 显示预估收益
  - [ ] 交易时间/非交易时间切换显示
- [ ] WebSocket订阅
  - [ ] 订阅用户持仓的所有基金
  - [ ] 实时更新列表数据

### Day 2: 持仓收益汇总 + 优化
- [ ] 整体收益汇总卡片
  - [ ] 总持仓市值（基于估值）
  - [ ] 当日预估收益
  - [ ] 累计收益
  - [ ] 收益率
- [ ] 交互优化
  - [ ] 刷新按钮
  - [ ] 最后更新时间
  - [ ] 数据过期提示
- [ ] 非交易时间显示
  - [ ] 显示上一交易日数据
  - [ ] 提示文案

---

## 核心代码

### 后端API
```java
@GetMapping("/api/portfolio/holdings-with-estimate")
public ApiResponse<PortfolioSummaryVO> getHoldingsWithEstimate() {
    Long userId = getCurrentUserId();
    
    // 1. 获取持仓列表
    List<PortfolioHolding> holdings = portfolioService.getHoldings(userId);
    
    // 2. 获取实时估值
    List<String> fundCodes = holdings.stream()
        .map(PortfolioHolding::getFundCode)
        .collect(Collectors.toList());
    
    Map<String, IntradayEstimate> estimates = estimateService
        .getLatestEstimates(fundCodes);
    
    // 3. 组装数据
    List<HoldingWithEstimateVO> list = holdings.stream().map(h -> {
        IntradayEstimate estimate = estimates.get(h.getFundCode());
        
        // 计算预估市值和收益
        BigDecimal estimateNav = estimate != null 
            ? estimate.getEstimateNav() 
            : h.getLastNav();
        
        BigDecimal estimateMarketValue = estimateNav
            .multiply(h.getHoldShares());
        
        BigDecimal estimateDailyReturn = estimate != null
            ? estimate.getEstimateChangePct()
                .multiply(h.getHoldShares())
                .multiply(h.getAvgCost())
                .divide(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        return HoldingWithEstimateVO.builder()
            .fundCode(h.getFundCode())
            .fundName(h.getFundName())
            .holdShares(h.getHoldShares())
            .avgCost(h.getAvgCost())
            .totalCost(h.getTotalCost())
            .estimateNav(estimateNav)
            .estimateChangePct(estimate?.getEstimateChangePct())
            .estimateMarketValue(estimateMarketValue)
            .estimateDailyReturn(estimateDailyReturn)
            .totalReturn(estimateMarketValue.subtract(h.getTotalCost()))
            .totalReturnPct(
                estimateMarketValue.subtract(h.getTotalCost())
                    .divide(h.getTotalCost(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
            )
            .lastUpdateTime(estimate?.getEstimateTime())
            .build();
    }).collect(Collectors.toList());
    
    // 4. 计算汇总
    BigDecimal totalCost = list.stream()
        .map(HoldingWithEstimateVO::getTotalCost)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalMarketValue = list.stream()
        .map(HoldingWithEstimateVO::getEstimateMarketValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalDailyReturn = list.stream()
        .map(HoldingWithEstimateVO::getEstimateDailyReturn)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalReturn = totalMarketValue.subtract(totalCost);
    
    BigDecimal totalReturnPct = totalCost.compareTo(BigDecimal.ZERO) > 0
        ? totalReturn.divide(totalCost, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
        : BigDecimal.ZERO;
    
    return ApiResponse.success(PortfolioSummaryVO.builder()
        .holdings(list)
        .summary(PortfolioSummary.builder()
            .totalCost(totalCost)
            .totalMarketValue(totalMarketValue)
            .totalDailyReturn(totalDailyReturn)
            .totalReturn(totalReturn)
            .totalReturnPct(totalReturnPct)
            .build())
        .isTradingTime(tradingCalendarService.isTradingTime())
        .build());
}
```

### 前端持仓列表
```vue
<template>
  <div class="portfolio-page">
    <!-- 汇总卡片 -->
    <div class="summary-card">
      <div class="summary-item">
        <span class="label">总持仓市值</span>
        <span class="value">{{ formatMoney(summary.totalMarketValue) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">当日预估收益</span>
        <span :class="['value', summary.totalDailyReturn >= 0 ? 'up' : 'down']">
          {{ summary.totalDailyReturn >= 0 ? '+' : '' }}
          {{ formatMoney(summary.totalDailyReturn) }}
        </span>
      </div>
      <div class="summary-item">
        <span class="label">累计收益</span>
        <span :class="['value', summary.totalReturn >= 0 ? 'up' : 'down']">
          {{ summary.totalReturn >= 0 ? '+' : '' }}
          {{ formatMoney(summary.totalReturn) }}
          ({{ summary.totalReturnPct.toFixed(2) }}%)
        </span>
      </div>
    </div>
    
    <!-- 持仓列表 -->
    <div class="holding-list">
      <div 
        v-for="item in holdings" 
        :key="item.fundCode"
        class="holding-item"
      >
        <div class="fund-info">
          <div class="fund-name">{{ item.fundName }}</div>
          <div class="hold-shares">持有 {{ item.holdShares }} 份</div>
        </div>
        
        <div class="estimate-info">
          <!-- 交易时间显示实时估值 -->
          <template v-if="isTradingTime">
            <div class="estimate-nav">{{ item.estimateNav.toFixed(4) }}</div>
            <div :class="['change-pct', item.estimateChangePct >= 0 ? 'up' : 'down']">
              {{ item.estimateChangePct >= 0 ? '+' : '' }}
              {{ item.estimateChangePct.toFixed(2) }}%
            </div>
            
            <div class="daily-return">
              今日: {{ item.estimateDailyReturn >= 0 ? '+' : '' }}
              {{ formatMoney(item.estimateDailyReturn) }}
            </div>
          </template>
          
          <!-- 非交易时间显示上一交易日 -->
          <template v-else>
            <div class="last-nav">{{ item.estimateNav.toFixed(4) }}</div>
            
            <div :class="['change-pct', item.estimateChangePct >= 0 ? 'up' : 'down']">
              {{ item.estimateChangePct >= 0 ? '+' : '' }}
              {{ item.estimateChangePct.toFixed(2) }}%
            </div>
            
            <div class="last-trade-hint">上一交易日</div>
          </template>
        </div>
        
        <div class="total-return">
          <div :class="['return-amt', item.totalReturn >= 0 ? 'up' : 'down']">
            {{ item.totalReturn >= 0 ? '+' : '' }}
            {{ formatMoney(item.totalReturn) }}
          </div>
          <div :class="['return-pct', item.totalReturnPct >= 0 ? 'up' : 'down']">
            {{ item.totalReturnPct >= 0 ? '+' : '' }}
            {{ item.totalReturnPct.toFixed(2) }}%
          </div>
        </div>
      </div>
    </div>
    
    <!-- 刷新按钮 -->
    <div class="refresh-bar">
      <el-button @click="refreshData" :loading="loading">
        刷新数据
      </el-button>
      <span class="update-time">最后更新: {{ lastUpdateTime }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useWebSocket } from '@vueuse/core'

const holdings = ref([])
const summary = ref({})
const isTradingTime = ref(false)
const loading = ref(false)
const lastUpdateTime = ref('')

// WebSocket订阅持仓更新
const { data: wsData } = useWebSocket('/ws/portfolio', {
  onConnected: () => {
    console.log('持仓WebSocket已连接')
  }
})

// 接收实时更新
watch(wsData, (newData) => {
  if (!newData) return
  const update = JSON.parse(newData)
  
  // 更新对应基金的估值
  const index = holdings.value.findIndex(
    h => h.fundCode === update.fundCode
  )
  if (index >= 0) {
    holdings.value[index].estimateNav = update.estimateNav
    holdings.value[index].estimateChangePct = update.estimateChangePct
    // 重新计算收益...
  }
})

// 加载数据
const loadData = async () => {
  loading.value = true
  const res = await fetch('/api/portfolio/holdings-with-estimate')
  const result = await res.json()
  
  holdings.value = result.data.holdings
  summary.value = result.data.summary
  isTradingTime.value = result.data.isTradingTime
  lastUpdateTime.value = new Date().toLocaleTimeString()
  
  loading.value = false
}

onMounted(loadData)
</script>
```

---

## 验收标准

- [ ] 持仓列表显示实时估值
- [ ] 交易时间/非交易时间切换正常
- [ ] 预估收益计算准确
- [ ] 整体收益汇总正确
- [ ] WebSocket实时更新正常
- [ ] 刷新功能可用

---

## 测试计划

测试日志: P5-05-test-log.md

---

**制定日期**: 2026-03-02
