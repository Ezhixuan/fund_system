# Phase 5 交互细节补充说明

## 更新日期
2026-03-02

---

## 1. 动态绘制折线图（交易日实时更新）

### 交互流程

```
用户打开基金详情页（交易日 10:00）
    ↓
前端请求历史数据（当日已采集的点位）
    ↓
ECharts 绘制基础折线图（9:30-10:00的线段）
    ↓
建立 WebSocket 连接
    ↓
每隔10分钟收到新数据推送
    ↓
动态追加数据点，图表实时延伸
    ↓
收盘时（15:00）形成完整当日分时图
```

### 前端实现

```vue
<template>
  <div class="intraday-chart">
    <!-- 图表容器 -->
    <v-chart ref="chartRef" :option="chartOption" />
    
    <!-- 手动刷新按钮 -->
    <button @click="manualRefresh" :loading="refreshing">
      🔄 刷新估值
    </button>
    
    <!-- 最后更新时间 -->
    <span class="update-time">
      最后更新: {{ lastUpdateTime }}
    </span>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useWebSocket } from '@vueuse/core'
import * as echarts from 'echarts'

const props = defineProps({
  fundCode: String
})

const chartRef = ref(null)
const chartData = ref([])  // 图表数据
const lastUpdateTime = ref('')
const refreshing = ref(false)

// 初始化图表
const initChart = async () => {
  // 1. 获取当日历史数据（已采集的点位）
  const historyData = await fetchIntradayHistory(props.fundCode)
  
  // 2. 填充数据
  chartData.value = historyData.map(item => ({
    time: item.estimateTime,
    value: item.estimateChangePct  // 显示涨跌幅
  }))
  
  // 3. 初始化 ECharts
  chartOption.value = {
    xAxis: {
      type: 'time',
      min: '09:30',
      max: '15:00',
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: { formatter: '{value}%' }
    },
    series: [{
      type: 'line',
      data: chartData.value,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2 },
      areaStyle: {
        opacity: 0.1
      }
    }, {
      // 昨日收盘参考线（0%线）
      type: 'line',
      markLine: {
        silent: true,
        data: [{ yAxis: 0 }],
        lineStyle: { type: 'dashed', color: '#999' }
      }
    }]
  }
  
  lastUpdateTime.value = historyData[historyData.length - 1]?.time || ''
}

// WebSocket 实时接收更新
const { data: wsData } = useWebSocket(
  () => `/ws/fund/${props.fundCode}/intraday`,
  {
    autoReconnect: true,
    heartbeat: true
  }
)

watch(wsData, (newData) => {
  if (!newData) return
  
  const update = JSON.parse(newData)
  
  // 动态追加数据点
  const newPoint = {
    time: update.estimateTime,
    value: update.estimateChangePct
  }
  
  // 检查是否已存在该时间点的数据（避免重复）
  const exists = chartData.value.find(p => p.time === newPoint.time)
  if (!exists) {
    chartData.value.push(newPoint)
    
    // ECharts 动态更新
    chartRef.value.setOption({
      series: [{
        data: chartData.value
      }]
    })
    
    lastUpdateTime.value = update.estimateTime
  }
})

// 手动刷新
const manualRefresh = async () => {
  refreshing.value = true
  
  try {
    // 调用后端接口，触发Python采集最新数据
    const result = await fetch('/api/fund/' + props.fundCode + '/estimate/refresh', {
      method: 'POST'
    })
    
    if (result.ok) {
      // 后端采集完成后会推送WebSocket，这里等待更新
      // 或轮询检查最新数据
      setTimeout(async () => {
        const latest = await fetchLatestEstimate(props.fundCode)
        if (latest) {
          // 手动追加到图表
          const newPoint = {
            time: latest.estimateTime,
            value: latest.estimateChangePct
          }
          
          // 更新或追加
          const index = chartData.value.findIndex(p => p.time === newPoint.time)
          if (index >= 0) {
            chartData.value[index] = newPoint  // 更新已有
          } else {
            chartData.value.push(newPoint)     // 追加新点
          }
          
          chartRef.value.setOption({
            series: [{ data: chartData.value }]
          })
          
          lastUpdateTime.value = latest.estimateTime
        }
      }, 3000)  // 等待3秒让Python完成采集
    }
  } finally {
    refreshing.value = false
  }
}

onMounted(initChart)
</script>
```

---

## 2. 手动刷新功能优化

### 当前问题
后端直接查询数据库返回缓存数据，不是最新数据。

### 优化方案

#### 后端 API 改造

```java
@RestController
@RequestMapping("/api/fund")
public class FundEstimateController {
    
    @Autowired
    private PythonCollectClient pythonClient;
    
    @Autowired
    private IntradayEstimateService estimateService;
    
    /**
     * 获取最新估值（优先数据库，可指定是否强制刷新）
     */
    @GetMapping("/{fundCode}/estimate")
    public ApiResponse<IntradayEstimateVO> getEstimate(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "false") boolean forceRefresh) {
        
        // 1. 检查数据库是否有最近2分钟的数据
        IntradayEstimateVO cached = estimateService.getLatest(fundCode);
        
        if (!forceRefresh && cached != null && 
            cached.getUpdateTime().plusMinutes(2).isAfter(LocalDateTime.now())) {
            // 2分钟内数据，直接返回
            return ApiResponse.success(cached);
        }
        
        // 2. 需要刷新，异步调用Python采集
        if (forceRefresh || cached == null) {
            // 异步执行，不阻塞用户
            CompletableFuture.runAsync(() -> {
                try {
                    // 调用Python采集服务
                    EstimateData data = pythonClient.collectEstimate(fundCode);
                    
                    // 保存到数据库
                    estimateService.save(data);
                    
                    // WebSocket推送更新
                    websocketService.pushEstimateUpdate(data);
                    
                } catch (Exception e) {
                    log.error("采集失败 {}: {}", fundCode, e.getMessage());
                }
            });
            
            // 3. 如果有缓存，先返回缓存数据
            if (cached != null) {
                cached.setRefreshing(true);  // 标记正在刷新
                return ApiResponse.success(cached);
            }
        }
        
        return ApiResponse.error("暂无数据");
    }
    
    /**
     * 手动刷新估值（强制触发Python采集）
     */
    @PostMapping("/{fundCode}/estimate/refresh")
    public ApiResponse<String> refreshEstimate(@PathVariable String fundCode) {
        // 1. 检查是否在交易时间
        if (!tradingCalendarService.isTradingTime()) {
            return ApiResponse.error("当前非交易时间");
        }
        
        // 2. 检查冷却时间（防止频繁刷新，30秒冷却）
        String cacheKey = "refresh:cooldown:" + fundCode;
        if (redisTemplate.hasKey(cacheKey)) {
            Long ttl = redisTemplate.getExpire(cacheKey);
            return ApiResponse.error("请" + ttl + "秒后再刷新");
        }
        
        // 3. 设置冷却时间
        redisTemplate.opsForValue().set(cacheKey, "1", 30, TimeUnit.SECONDS);
        
        // 4. 触发采集
        CompletableFuture.runAsync(() -> {
            try {
                EstimateData data = pythonClient.collectEstimate(fundCode);
                estimateService.save(data);
                websocketService.pushEstimateUpdate(data);
            } catch (Exception e) {
                log.error("刷新失败 {}: {}", fundCode, e.getMessage());
            }
        });
        
        return ApiResponse.success("刷新请求已提交，请稍后查看");
    }
}
```

#### Python 采集客户端

```python
# collector/api_service.py (Flask)

@app.route('/api/collect/estimate', methods=['POST'])
def collect_estimate():
    """实时采集单只基金估值"""
    fund_code = request.json.get('fundCode')
    
    try:
        # 尝试多个数据源
        for source in get_data_sources():
            try:
                data = source.collect_estimate(fund_code)
                if validate_estimate(data):
                    return jsonify({
                        'success': True,
                        'data': {
                            'fundCode': fund_code,
                            'estimateTime': datetime.now().isoformat(),
                            'estimateNav': data['nav'],
                            'estimateChangePct': data['change_pct'],
                            'preCloseNav': data['pre_close'],
                            'dataSource': source.name
                        }
                    })
            except Exception as e:
                logger.warning(f"{source.name} 失败: {e}")
                continue
        
        return jsonify({'success': False, 'error': '所有数据源均失败'})
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)})
```

#### 前端交互优化

```vue
<template>
  <div class="estimate-display">
    <div class="main-value">
      <span class="nav">{{ estimate.estimateNav }}</span>
      <span :class="['change', estimate.changePct >= 0 ? 'up' : 'down']">
        {{ estimate.changePct >= 0 ? '+' : '' }}{{ estimate.changePct }}%
      </span>
    </div>
    
    <div class="actions">
      <!-- 刷新按钮 -->
      <el-button 
        @click="handleRefresh"
        :loading="refreshing"
        :disabled="cooldown > 0"
        size="small"
      >
        <span v-if="cooldown > 0">{{ cooldown }}s后可刷新</span>
        <span v-else>🔄 刷新估值</span>
      </el-button>
      
      <!-- 更新时间 -->
      <span class="update-time">
        更新于: {{ formatTime(estimate.updateTime) }}
        <span v-if="estimate.refreshing" class="refreshing-tag">刷新中...</span>
      </span>
    </div>
  </div>
</template>

<script setup>
const estimate = ref({})
const refreshing = ref(false)
const cooldown = ref(0)

// 自动刷新冷却倒计时
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

const handleRefresh = async () => {
  refreshing.value = true
  
  try {
    const result = await fetch(`/api/fund/${fundCode}/estimate/refresh`, {
      method: 'POST'
    }).then(r => r.json())
    
    if (result.success) {
      // 等待WebSocket推送更新
      // 或显示提示
      ElMessage.success('刷新请求已提交')
      startCooldown(30)  // 30秒冷却
    } else {
      ElMessage.warning(result.message)
    }
  } finally {
    refreshing.value = false
  }
}

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>
```

---

## 3. 折线图跨交易日处理

### 业务逻辑

```
场景1: 用户在交易日（周一）打开详情页
    ↓
查询周一的当日点位数据
    ↓
绘制周一的分时图
    ↓
收盘后（15:00）图表定格

场景2: 用户在周一收盘后打开详情页
    ↓
查询周一的当日点位数据（完整）
    ↓
显示周一的分时图（历史数据）
    ↓
标注"已收盘"

场景3: 用户在周二开盘前打开详情页（跨交易日）
    ↓
检测: 当前是周二 9:00，最新数据是周一 15:00
    ↓
显示策略:
  1. 显示周一的完整分时图
  2. 清空图表标题为"上一交易日（周一）"
  3. 9:30开盘后，自动切换到周二数据

场景4: 用户在周二开盘后打开详情页
    ↓
检测到新交易日（trade_date变化）
    ↓
清空图表数据
    ↓
从9:30开始重新绘制新的分时图
```

### 前端实现

```vue
<script setup>
const currentTradeDate = ref('')  // 当前显示的交易日
const chartData = ref([])

// 初始化或切换交易日
const initOrSwitchTradeDate = async () => {
  // 1. 获取服务器当前交易日
  const serverTradeDate = await fetchCurrentTradeDate()
  
  // 2. 检查是否切换了交易日
  if (currentTradeDate.value && currentTradeDate.value !== serverTradeDate) {
    // 交易日切换，清空数据重新加载
    console.log(`交易日切换: ${currentTradeDate.value} → ${serverTradeDate}`)
    chartData.value = []
    currentTradeDate.value = serverTradeDate
  } else if (!currentTradeDate.value) {
    currentTradeDate.value = serverTradeDate
  }
  
  // 3. 加载该交易日的数据
  await loadIntradayData(serverTradeDate)
}

// 定时检查交易日切换（每分钟检查）
setInterval(() => {
  initOrSwitchTradeDate()
}, 60000)

// WebSocket接收数据时检查
watch(wsData, (newData) => {
  const update = JSON.parse(newData)
  
  // 检查数据是否属于当前交易日
  if (update.tradeDate !== currentTradeDate.value) {
    // 检测到新交易日，刷新页面或清空重绘
    initOrSwitchTradeDate()
    return
  }
  
  // 正常追加数据
  appendDataPoint(update)
})

// 页面可见性变化时检查（处理用户隔夜打开页面）
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') {
    initOrSwitchTradeDate()
  }
})
</script>
```

### 后端 API

```java
@GetMapping("/api/fund/{fundCode}/intraday")
public ApiResponse<IntradayDataVO> getIntradayData(
        @PathVariable String fundCode,
        @RequestParam(required = false) String tradeDate) {
    
    // 1. 如果没有指定日期，获取当前交易日
    if (tradeDate == null) {
        tradeDate = tradingCalendarService.getCurrentTradeDate();
    }
    
    // 2. 查询该交易日的所有点位数据
    List<IntradayEstimate> points = estimateService
        .getIntradayPoints(fundCode, tradeDate);
    
    // 3. 获取上一交易日收盘净值（参考线）
    String preTradeDate = tradingCalendarService
        .getPreviousTradeDate(tradeDate);
    BigDecimal preCloseNav = navService
        .getCloseNav(fundCode, preTradeDate);
    
    return ApiResponse.success(IntradayDataVO.builder()
        .tradeDate(tradeDate)
        .points(points)
        .preCloseNav(preCloseNav)
        .isToday(tradeDate.equals(LocalDate.now().toString()))
        .build());
}
```

---

## 总结

### 三个核心交互点

1. **动态绘制**: WebSocket推送 → ECharts实时追加数据点 → 折线图动态延伸
2. **手动刷新**: 按钮触发 → 后端调用Python采集 → WebSocket推送更新 → 前端更新
3. **跨交易日**: 检测trade_date变化 → 清空图表 → 加载新数据重新绘制

### 关键状态管理

```typescript
interface IntradayChartState {
  fundCode: string
  currentTradeDate: string    // 当前显示的交易日
  chartData: DataPoint[]      // 图表数据点
  isRealtime: boolean         // 是否实时更新中
  lastUpdateTime: string      // 最后更新时间
  refreshing: boolean         // 是否正在手动刷新
  cooldown: number            // 刷新冷却倒计时
}
```
