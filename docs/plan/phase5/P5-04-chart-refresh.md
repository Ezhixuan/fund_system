# P5-04: 分时图与手动刷新

## 任务信息
| 属性 | 值 |
|------|------|
| 任务ID | P5-04 |
| 名称 | 分时图与手动刷新 |
| 状态 | ⏳ 待开始 |
| 计划工期 | 3天 |
| 依赖 | P5-03（WebSocket推送）|

---

## 需求描述

实现分时图动态绘制和手动刷新功能：
1. ECharts 分时图实时绘制（动态追加数据点）
2. 手动刷新按钮（触发Python采集，30秒冷却）
3. 跨交易日处理（自动检测切换，清空重绘）

---

## 实现步骤

### Day 1: ECharts 分时图组件
- [ ] IntradayChart 组件
  - [ ] ECharts 基础配置
  - [ ] 时间轴（9:30-15:00）
  - [ ] 涨跌幅Y轴
  - [ ] 昨日收盘参考线（0%线）
  - [ ] 数据点样式
- [ ] 图表数据管理
  - [ ] 初始化加载历史数据
  - [ ] WebSocket数据追加
  - [ ] 动态更新动画

### Day 2: 手动刷新功能
- [ ] 后端API
  - [ ] POST /api/fund/{code}/estimate/refresh
  - [ ] 调用Python采集
  - [ ] 30秒冷却机制（Redis）
  - [ ] 异步处理 + WebSocket推送
- [ ] 前端刷新按钮
  - [ ] 刷新按钮组件
  - [ ] 冷却倒计时显示
  - [ ] 刷新状态提示
  - [ ] 结果反馈

### Day 3: 跨交易日处理
- [ ] 交易日检测
  - [ ] 定时检查 trade_date
  - [ ] 页面可见性变化检测
- [ ] 图表清空重绘
  - [ ] 检测到新交易日
  - [ ] 清空图表数据
  - [ ] 加载新数据
  - [ ] 提示"新交易日已开始"
- [ ] 历史分时查看
  - [ ] 查看上一交易日分时
  - [ ] 日期选择器

---

## 核心代码

### 分时图组件
```vue
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
import { useIntradayWebSocket } from '@/composables/useIntradayWebSocket'

const props = defineProps({
  fundCode: String
})

const chartContainer = ref(null)
let chart = null
const chartData = ref([])
const currentTradeDate = ref('')
const lastUpdateTime = ref('')
const refreshing = ref(false)
const cooldown = ref(0)

// 是否当前交易日
const isToday = computed(() => {
  return currentTradeDate.value === new Date().toISOString().slice(0, 10)
})

// 初始化图表
const initChart = async () => {
  // 1. 获取服务器当前交易日
  const serverDate = await fetchCurrentTradeDate()
  
  // 2. 检查是否切换交易日
  if (currentTradeDate.value && currentTradeDate.value !== serverDate) {
    console.log(`交易日切换: ${currentTradeDate.value} → ${serverDate}`)
    chartData.value = []
  }
  currentTradeDate.value = serverDate
  
  // 3. 加载数据
  const history = await fetchIntradayHistory(props.fundCode, serverDate)
  chartData.value = history.map(item => ({
    time: item.estimateTime,
    value: item.estimateChangePct
  }))
  
  // 4. ECharts配置
  chart = echarts.init(chartContainer.value)
  
  const option = {
    grid: {
      top: 20,
      right: 20,
      bottom: 30,
      left: 50
    },
    xAxis: {
      type: 'time',
      min: `${serverDate} 09:30:00`,
      max: `${serverDate} 15:00:00`,
      splitLine: { show: false },
      axisLabel: {
        formatter: '{HH}:{mm}'
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: {
        formatter: '{value}%'
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#eee'
        }
      }
    },
    series: [
      {
        type: 'line',
        data: chartData.value,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 2,
          color: (params) => params.value >= 0 ? '#ff4d4f' : '#52c41a'
        },
        itemStyle: {
          color: (params) => params.value >= 0 ? '#ff4d4f' : '#52c41a'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 77, 79, 0.1)' },
            { offset: 1, color: 'rgba(255, 77, 79, 0)' }
          ])
        }
      },
      {
        type: 'line',
        markLine: {
          silent: true,
          data: [{ yAxis: 0 }],
          lineStyle: {
            type: 'dashed',
            color: '#999'
          },
          label: {
            formatter: '0%'
          }
        }
      }
    ]
  }
  
  chart.setOption(option)
  
  if (chartData.value.length > 0) {
    lastUpdateTime.value = chartData.value[chartData.value.length - 1].time
  }
}

// WebSocket实时更新
const { data: wsData } = useIntradayWebSocket(props.fundCode)

watch(wsData, (newData) => {
  if (!newData) return
  
  const update = JSON.parse(newData)
  
  // 检查是否当前交易日
  if (update.tradeDate !== currentTradeDate.value) {
    initChart()  // 重新初始化
    return
  }
  
  // 追加数据点
  const newPoint = {
    time: update.estimateTime,
    value: update.estimateChangePct
  }
  
  // 检查是否已存在
  const exists = chartData.value.find(p => p.time === newPoint.time)
  if (!exists) {
    chartData.value.push(newPoint)
    
    // 动态更新
    chart.setOption({
      series: [{
        data: chartData.value
      }]
    })
    
    lastUpdateTime.value = update.estimateTime
  }
})

// 手动刷新
const handleRefresh = async () => {
  refreshing.value = true
  
  try {
    const res = await fetch(`/api/fund/${props.fundCode}/estimate/refresh`, {
      method: 'POST'
    })
    
    const result = await res.json()
    
    if (result.code === 200) {
      ElMessage.success('刷新请求已提交')
      startCooldown(30)
    } else {
      ElMessage.warning(result.message)
    }
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

// 定时检查交易日切换
let checkTimer = null
const startTradeDateCheck = () => {
  checkTimer = setInterval(async () => {
    const serverDate = await fetchCurrentTradeDate()
    if (serverDate !== currentTradeDate.value) {
      console.log('检测到新交易日')
      initChart()
    }
  }, 60000)  // 每分钟检查
}

// 页面可见性变化检测
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') {
    initChart()
  }
})

onMounted(() => {
  initChart()
  startTradeDateCheck()
})

onUnmounted(() => {
  if (chart) chart.dispose()
  if (cooldownTimer) clearInterval(cooldownTimer)
  if (checkTimer) clearInterval(checkTimer)
})
</script>
```

---

## 后端手动刷新API

```java
@RestController
@RequestMapping("/api/fund")
public class FundEstimateController {
    
    @Autowired
    private PythonCollectClient pythonClient;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @PostMapping("/{fundCode}/estimate/refresh")
    public ApiResponse<String> refreshEstimate(@PathVariable String fundCode) {
        // 1. 检查是否在交易时间
        if (!tradingCalendarService.isTradingTime()) {
            return ApiResponse.error("当前非交易时间，无法刷新");
        }
        
        // 2. 检查冷却时间（30秒）
        String cooldownKey = "refresh:cooldown:" + fundCode + ":" + getCurrentUserId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttl = redisTemplate.getExpire(cooldownKey);
            return ApiResponse.error("请" + ttl + "秒后再刷新");
        }
        
        // 3. 设置冷却
        redisTemplate.opsForValue().set(cooldownKey, "1", 30, TimeUnit.SECONDS);
        
        // 4. 异步触发采集
        CompletableFuture.runAsync(() -> {
            try {
                // 调用Python采集
                EstimateData data = pythonClient.collectEstimate(fundCode);
                
                // 保存数据库
                estimateService.save(data);
                
                // WebSocket推送
                pushService.pushToFundDetail(data);
                
            } catch (Exception e) {
                log.error("刷新失败 {}: {}", fundCode, e.getMessage());
            }
        });
        
        return ApiResponse.success("刷新请求已提交，请稍后查看");
    }
}
```

---

## 验收标准

- [ ] 分时图正常显示
- [ ] WebSocket数据动态追加
- [ ] 手动刷新按钮可用
- [ ] 30秒冷却机制生效
- [ ] 跨交易日自动切换
- [ ] 历史分时可查看

---

## 测试计划

测试日志: P5-04-test-log.md

---

**制定日期**: 2026-03-02
