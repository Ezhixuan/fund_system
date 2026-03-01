<template>
  <div class="intraday-chart">
    <div ref="chartContainer" class="chart-container"></div>
    
    <div class="chart-controls">
      <!-- 刷新按钮 -->
      <el-button 
        @click="handleRefresh"
        :loading="refreshing"
        :disabled="cooldown > 0"
        size="small"
        type="primary"
      >
        <span v-if="cooldown > 0">{{ cooldown }}s后可刷新</span>
        <span v-else>🔄 刷新估值</span>
      </el-button>
      
      <!-- 更新时间 -->
      <span class="update-info">
        更新于: {{ formatTime(lastUpdateTime) }}
        <span v-if="isToday" class="today-tag">今日</span>
        <span v-else class="history-tag">{{ tradeDate }}</span>
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { useIntradayWebSocket } from '@/composables/useIntradayWebSocket'
import { refreshEstimate } from '@/api/fund'

const props = defineProps({
  fundCode: {
    type: String,
    required: true
  }
})

const chartContainer = ref(null)
let chart = null

// 图表数据
const chartData = ref([])
const currentTradeDate = ref('')
const lastUpdateTime = ref('')
const refreshing = ref(false)
const cooldown = ref(0)

// 是否当前交易日
const isToday = computed(() => {
  return currentTradeDate.value === new Date().toISOString().slice(0, 10)
})

const tradeDate = computed(() => {
  return currentTradeDate.value
})

// 初始化图表
const initChart = async () => {
  if (!chartContainer.value) return
  
  // 初始化 ECharts
  chart = echarts.init(chartContainer.value)
  
  // 设置基础配置
  const option = {
    grid: {
      top: 40,
      right: 20,
      bottom: 30,
      left: 50
    },
    tooltip: {
      trigger: 'axis',
      formatter: function(params) {
        const data = params[0]
        return `${data.name}<br/>涨跌幅: ${data.value >= 0 ? '+' : ''}${data.value}%`
      }
    },
    xAxis: {
      type: 'category',
      data: [],
      axisLabel: {
        formatter: (value) => {
          return value.slice(11, 16) // 只显示时:分
        }
      },
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: '#eee'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '{value}%'
      },
      splitLine: {
        lineStyle: {
          color: '#eee'
        }
      }
    },
    series: [
      {
        type: 'line',
        data: [],
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 2
        },
        itemStyle: {
          color: (params) => {
            return params.value >= 0 ? '#ff4d4f' : '#52c41a'
          }
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 77, 79, 0.1)' },
            { offset: 1, color: 'rgba(255, 77, 79, 0)' }
          ])
        },
        markLine: {
          silent: true,
          data: [
            {
              yAxis: 0,
              lineStyle: {
                type: 'dashed',
                color: '#999'
              },
              label: {
                formatter: '0%'
              }
            }
          ]
        }
      }
    ]
  }
  
  chart.setOption(option)
  
  // 加载初始数据
  await loadData()
}

// 加载数据
const loadData = async () => {
  try {
    // 获取当日分时数据
    const response = await fetch(`/api/fund/${props.fundCode}/intraday`)
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      currentTradeDate.value = result.data.tradeDate
      
      if (result.data.points && result.data.points.length > 0) {
        chartData.value = result.data.points.map(item => ({
          time: item.estimateTime,
          value: item.estimateChangePct
        }))
        
        updateChart()
        lastUpdateTime.value = chartData.value[chartData.value.length - 1].time
      }
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

// 更新图表
const updateChart = () => {
  if (!chart) return
  
  const times = chartData.value.map(item => item.time)
  const values = chartData.value.map(item => item.value)
  
  chart.setOption({
    xAxis: {
      data: times
    },
    series: [{
      data: values
    }]
  })
}

// WebSocket实时更新
const { data: wsData } = useIntradayWebSocket(props.fundCode)

watch(wsData, (newData) => {
  if (!newData) return
  
  const update = JSON.parse(newData)
  
  // 检查是否当前交易日
  if (update.tradeDate !== currentTradeDate.value) {
    // 交易日切换，重新加载
    loadData()
    return
  }
  
  // 查找是否已存在该时间点的数据
  const index = chartData.value.findIndex(p => p.time === update.estimateTime)
  
  if (index >= 0) {
    // 更新已有数据
    chartData.value[index].value = update.estimateChangePct
  } else {
    // 添加新数据
    chartData.value.push({
      time: update.estimateTime,
      value: update.estimateChangePct
    })
  }
  
  // 按时间排序
  chartData.value.sort((a, b) => new Date(a.time) - new Date(b.time))
  
  updateChart()
  lastUpdateTime.value = update.estimateTime
})

// 手动刷新
const handleRefresh = async () => {
  if (cooldown.value > 0) return
  
  refreshing.value = true
  
  try {
    const result = await refreshEstimate(props.fundCode)
    
    if (result.code === 200) {
      ElMessage.success('刷新请求已提交')
      startCooldown(30)
    } else {
      ElMessage.warning(result.message || '刷新失败')
    }
  } catch (error) {
    ElMessage.error('刷新失败: ' + error.message)
  } finally {
    refreshing.value = false
  }
}

// 冷却倒计时
let cooldownTimer = null
const startCooldown = (seconds) => {
  cooldown.value = seconds
  cooldownTimer = setInterval(() => {
    cooldown.value--
    if (cooldown.value <= 0) {
      clearInterval(cooldownTimer)
    }
  }, 1000)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 窗口大小变化时重绘
const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (cooldownTimer) clearInterval(cooldownTimer)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped lang="scss">
.intraday-chart {
  .chart-container {
    width: 100%;
    height: 300px;
  }

  .chart-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    padding: 0 8px;

    .update-info {
      font-size: 12px;
      color: #909399;

      .today-tag {
        margin-left: 8px;
        padding: 2px 6px;
        background: #e6f7ff;
        color: #1890ff;
        border-radius: 4px;
        font-size: 11px;
      }

      .history-tag {
        margin-left: 8px;
        padding: 2px 6px;
        background: #f5f5f5;
        color: #666;
        border-radius: 4px;
        font-size: 11px;
      }
    }
  }
}
</style>
