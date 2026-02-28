<template>
  <div class="fund-detail">
    <!-- 基本信息 -->
    <el-card v-loading="loading">
      <div class="fund-header">
        <div class="fund-title">
          <h1>{{ fundInfo.fundName }}</h1>
          <span class="fund-code">{{ fundInfo.fundCode }}</span>
        </div>
        <div class="fund-tags">
          <el-tag>{{ fundInfo.fundType || '未知类型' }}</el-tag>
          <el-tag v-if="fundInfo.riskLevel" :type="getRiskType(fundInfo.riskLevel)">
            风险等级 R{{ fundInfo.riskLevel }}
          </el-tag>
          <el-tag v-if="metrics.qualityLevel" :type="getQualityType(metrics.qualityLevel)">
            {{ metrics.qualityLevel }}级
          </el-tag>
        </div>      
      </div>
      
      <!-- 交易信号 -->
      <div v-if="signal" class="signal-box" :class="signal.signal?.toLowerCase()">
        <div class="signal-header">
          <span class="signal-type">{{ signal.signal }}</span>
          <span class="signal-confidence">置信度: {{ signal.confidence }}%</span>
        </div>
        <p class="signal-reason">{{ signal.reason }}</p>
      </div>
    </el-card>
    
    <!-- 指标卡片 -->
    <el-row :gutter="20" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-label">夏普比率</div>
          <div class="metric-value" :class="getMetricClass(metrics.sharpeRatio1y, 1)">
            {{ formatNumber(metrics.sharpeRatio1y) }}
          </div>
          <div class="metric-desc">>1.5优秀</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-label">最大回撤</div>
          <div class="metric-value" :class="getMetricClass(metrics.maxDrawdown1y, -20, true)">
            {{ formatNumber(metrics.maxDrawdown1y) }}%
          </div>
          <div class="metric-desc"><20%良好</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-label">近1年收益</div>
          <div class="metric-value" :class="metrics.return1y > 0 ? 'positive' : 'negative'">
            {{ formatPercent(metrics.return1y) }}
          </div>
          <div class="metric-desc">年化收益</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-label">波动率</div>
          <div class="metric-value">
            {{ formatNumber(metrics.volatility1y) }}%
          </div>
          <div class="metric-desc">年化波动</div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 净值曲线 -->
    <el-card class="chart-card">
      <template #header><span>净值走势</span></template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>
    
    <!-- 基金经理信息 -->
    <el-card v-if="fundInfo.managerName">
      <template #header><span>基金信息</span></template>
      <el-descriptions :column="2">
        <el-descriptions-item label="基金经理">{{ fundInfo.managerName }}</el-descriptions-item>
        <el-descriptions-item label="基金公司">{{ fundInfo.companyName }}</el-descriptions-item>
        <el-descriptions-item label="成立日期">{{ fundInfo.establishDate }}</el-descriptions-item>
        <el-descriptions-item label="业绩基准">{{ fundInfo.benchmark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
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

// 获取基金详情
const fetchFundDetail = async () => {
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
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>净值: ${p.value}`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      scale: true,
    },
    series: [
      {
        name: '单位净值',
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'none',
        lineStyle: {
          color: '#409eff',
          width: 2,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' },
          ]),
        },
      },
    ],
  }
  
  chart.setOption(option)
}

// 窗口大小改变时重新渲染
const handleResize = () => {
  chart?.resize()
}

// 工具函数
const getRiskType = (level) => {
  const types = ['', 'success', 'success', 'warning', 'danger', 'danger']
  return types[level] || 'info'
}

const getQualityType = (level) => {
  const types = { S: 'danger', A: 'success', B: 'primary', C: 'warning', D: 'info' }
  return types[level] || 'info'
}

const getMetricClass = (value, threshold, reverse = false) => {
  if (value === null || value === undefined) return ''
  const val = Number(value)
  const pass = reverse ? val > threshold : val >= threshold
  return pass ? 'positive' : 'negative'
}

onMounted(() => {
  fetchFundDetail()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.fund-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.fund-header {
  margin-bottom: 20px;
}

.fund-title {
  display: flex;
  align-items: baseline;
  gap: 15px;
  margin-bottom: 15px;
}

.fund-title h1 {
  font-size: 24px;
  color: #303133;
  margin: 0;
}

.fund-code {
  font-size: 16px;
  color: #909399;
}

.fund-tags {
  display: flex;
  gap: 10px;
}

.signal-box {
  margin-top: 20px;
  padding: 20px;
  border-radius: 8px;
  border: 2px solid;
}

.signal-box.buy {
  background-color: #f0f9eb;
  border-color: #67c23a;
}

.signal-box.hold {
  background-color: #f4f4f5;
  border-color: #909399;
}

.signal-box.sell {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

.signal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.signal-type {
  font-size: 20px;
  font-weight: bold;
}

.signal-box.buy .signal-type {
  color: #67c23a;
}

.signal-box.sell .signal-type {
  color: #f56c6c;
}

.signal-confidence {
  font-size: 14px;
  color: #606266;
}

.signal-reason {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

.metrics-row {
  margin-bottom: 20px;
}

.metric-card {
  text-align: center;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.metric-value.positive {
  color: #67c23a;
}

.metric-value.negative {
  color: #f56c6c;
}

.metric-desc {
  font-size: 12px;
  color: #c0c4cc;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-container {
  height: 350px;
}
</style>
