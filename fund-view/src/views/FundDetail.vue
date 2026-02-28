<template>
  <div class="fund-detail" v-loading="loading">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="$router.back()">
      ← 返回
    </button>
    
    <!-- 基金头部信息 -->
    <div class="fund-header">
      <div class="fund-title-section">
        <h1 class="fund-name">{{ fundInfo.fundName }}</h1>
        <div class="fund-meta">
          <span class="fund-code">{{ fundInfo.fundCode }}</span>
          <span v-if="fundInfo.fundType" class="tag">{{ fundInfo.fundType }}</span>
          <span v-if="metrics.qualityLevel" class="tag" :class="'tag-' + getQualityColor(metrics.qualityLevel)">
            {{ metrics.qualityLevel }}级
          </span>
        </div>
      </div>
    </div>
    
    <!-- 交易信号卡片 -->
    <div v-if="signal" class="signal-card" :class="'signal-' + signal.signal">
      <div class="signal-main">
        <div class="signal-icon">{{ signalIcons[signal.signal] }}</div>
        <div class="signal-content">
          <div class="signal-type">{{ signalText[signal.signal] }}</div>
          <div class="signal-confidence">置信度 {{ signal.confidence }}%</div>
        </div>
      </div>      
      <div class="signal-reason">{{ signal.reason }}</div>
    </div>
    
    <!-- 指标卡片网格 -->
    <div class="metrics-grid">
      <div class="metric-item">
        <div class="metric-label">夏普比率</div>
        <div class="metric-value" :class="getMetricClass(metrics.sharpeRatio1y, 1)">
          {{ formatNumber(metrics.sharpeRatio1y) }}
        </div>
        <div class="metric-hint">>1.5 优秀</div>
      </div>
      
      <div class="metric-item">
        <div class="metric-label">最大回撤</div>
        <div class="metric-value negative">
          {{ formatNumber(metrics.maxDrawdown1y) }}%
        </div>
        <div class="metric-hint">越小越好</div>
      </div>
      
      <div class="metric-item">
        <div class="metric-label">近1年收益</div>
        <div class="metric-value" :class="metrics.return1y > 0 ? 'positive' : 'negative'">
          {{ formatPercent(metrics.return1y) }}
        </div>
        <div class="metric-hint">年化收益</div>
      </div>
      
      <div class="metric-item">
        <div class="metric-label">波动率</div>
        <div class="metric-value">
          {{ formatNumber(metrics.volatility1y) }}%
        </div>
        <div class="metric-hint">年化波动</div>
      </div>
    </div>
    
    <!-- 净值曲线 -->
    <div class="chart-section">
      <div class="section-title">净值走势</div>      
      <div ref="chartRef" class="chart-container"></div>
    </div>
    
    <!-- 基金信息 -->
    <div class="info-section">
      <div class="section-title">基金信息</div>
      
      <div class="info-grid">
        <div class="info-item">
          <div class="info-item-label">基金经理</div>
          <div class="info-item-value">{{ fundInfo.managerName || '-' }}</div>
        </div>
        
        <div class="info-item">
          <div class="info-item-label">基金公司</div>
          <div class="info-item-value">{{ fundInfo.companyName || '-' }}</div>
        </div>
        
        <div class="info-item">
          <div class="info-item-label">成立日期</div>
          <div class="info-item-value">{{ fundInfo.establishDate || '-' }}</div>
        </div>
        
        <div class="info-item">
          <div class="info-item-label">业绩基准</div>
          <div class="info-item-value">{{ fundInfo.benchmark || '-' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { fundApi } from '@/api'
import { formatNumber, formatPercent } from '@/utils'

const route = useRoute()
const fundCode = route.params.code

const loading = ref(false)
const fundInfo = ref({})
const metrics = ref({})
const signal = ref(null)
const navData = ref([])

const chartRef = ref(null)
let chart = null

const signalIcons = {
  BUY: '📈',
  HOLD: '⏸️',
  SELL: '📉',
}

const signalText = {
  BUY: '建议买入',
  HOLD: '建议持有',
  SELL: '建议卖出',
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const [infoRes, metricsRes, signalRes, navRes] = await Promise.all([
      fundApi.getFundDetail(fundCode),
      fundApi.getFundMetrics(fundCode),
      fundApi.getFundSignal(fundCode),
      fundApi.getFundNav(fundCode, { days: 365 }),
    ])
    
    if (infoRes.success) fundInfo.value = infoRes.data
    if (metricsRes.success) metrics.value = metricsRes.data
    if (signalRes.success) signal.value = signalRes.data
    if (navRes.success) navData.value = navRes.data || []
    
    nextTick(() => {
      initChart()
    })
  } finally {
    loading.value = false
  }
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value || navData.value.length === 0) return
  
  chart = echarts.init(chartRef.value)
  
  const dates = navData.value.map(item => item.navDate).reverse()
  const values = navData.value.map(item => item.unitNav).reverse()
  
  const option = {
    grid: {
      left: 0,
      right: 0,
      top: 10,
      bottom: 20,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e1e8ed',
      borderWidth: 1,
      textStyle: {
        color: '#0f1419',
      },
      formatter: (params) => {
        const p = params[0]
        return `<div style="font-weight:600">${p.name}</div>
                <div style="color:#00acee">净值: ${p.value}</div>`
      },
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#536471',
        fontSize: 11,
      },
    },
    yAxis: {
      type: 'value',
      scale: true,
      splitLine: {
        lineStyle: {
          color: '#eff3f4',
        },
      },
      axisLabel: {
        color: '#536471',
        fontSize: 11,
      },
    },
    series: [
      {
        name: '单位净值',
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'none',
        lineStyle: {
          color: '#00acee',
          width: 3,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 172, 238, 0.25)' },
            { offset: 1, color: 'rgba(0, 172, 238, 0.02)' },
          ]),
        },
      },
    ],
  }
  
  chart.setOption(option)
}

const handleResize = () => {
  chart?.resize()
}

const getQualityColor = (level) => {
  const colors = { S: 'danger', A: 'success', B: 'primary', C: 'warning', D: 'info' }
  return colors[level] || 'primary'
}

const getMetricClass = (value, threshold) => {
  if (value === null || value === undefined) return ''
  return Number(value) >= threshold ? 'positive' : ''
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.fund-detail {
  max-width: 800px;
  margin: 0 auto;
}

/* 返回按钮 */
.back-btn {
  margin-bottom: 20px;
  padding: 10px 20px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.back-btn:hover {
  background: var(--bg-hover);
  color: var(--primary-color);
}

/* 基金头部 */
.fund-header {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 28px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color);
}

.fund-name {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.fund-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.fund-code {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 600;
}

/* 信号卡片 */
.signal-card {
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
  border: 2px solid;
  transition: var(--transition);
}

.signal-BUY {
  background: rgba(0, 186, 124, 0.08);
  border-color: #00ba7c;
}

.signal-HOLD {
  background: rgba(83, 100, 113, 0.08);
  border-color: #536471;
}

.signal-SELL {
  background: rgba(244, 33, 46, 0.08);
  border-color: #f4212e;
}

.signal-main {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.signal-icon {
  font-size: 40px;
}

.signal-type {
  font-size: 22px;
  font-weight: 800;
}

.signal-BUY .signal-type { color: #00ba7c; }
.signal-HOLD .signal-type { color: #536471; }
.signal-SELL .signal-type { color: #f4212e; }

.signal-confidence {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 600;
}

.signal-reason {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.6;
  padding-left: 56px;
}

/* 指标网格 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.metric-item {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.metric-item:hover {
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.metric-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.metric-value.positive {
  color: #00ba7c;
}

.metric-value.negative {
  color: #f4212e;
}

.metric-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 图表区域 */
.chart-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color);
}

.section-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
}

/* 信息区域 */
.info-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.info-item-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.info-item-value {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 700;
}

/* 响应式 */
@media (max-width: 768px) {
  .fund-name {
    font-size: 20px;
  }
  
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .metric-value {
    font-size: 22px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
