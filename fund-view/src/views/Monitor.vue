<template>
  <div class="monitor-page">
    <h1>🔍 系统监控面板</h1>
    
    <!-- 服务状态 -->
    <section class="monitor-section">
      <h2>服务状态</h2>
      <div class="service-grid">
        <div v-for="service in services" :key="service.name" 
             class="service-card" :class="service.status">
          <div class="service-name">{{ service.name }}</div>
          <div class="service-status">{{ service.status === 'healthy' ? '✅ 正常' : '❌ 异常' }}</div>
          <div class="service-time">{{ service.responseTime }}</div>
        </div>
      </div>
    </section>
    
    <!-- 数据覆盖情况 -->
    <section class="monitor-section">
      <h2>📊 数据覆盖情况</h2>
      <div class="data-stats">
        <div class="stat-card">
          <div class="stat-label">基金总数</div>
          <div class="stat-value">{{ dataStats.totalFunds }}</div>
        </div>
        <div class="stat-card"
             :class="{ 'warning': dataStats.fundInfoRate < 100 }">
          <div class="stat-label">基本信息完整度</div>
          <div class="stat-value">{{ dataStats.fundInfoRate }}%</div>
        </div>
        <div class="stat-card"
             :class="{ 'warning': dataStats.metricsRate < 80 }">
          <div class="stat-label">指标数据完整度</div>
          <div class="stat-value">{{ dataStats.metricsRate }}%</div>
        </div>
        <div class="stat-card"
             :class="{ 'warning': dataStats.navRate < 80 }">
          <div class="stat-label">NAV历史完整度</div>
          <div class="stat-value">{{ dataStats.navRate }}%</div>
        </div>
      </div>
    </section>
    
    <!-- API链路监控 -->
    <section class="monitor-section">
      <h2>🔗 API调用链路</h2>
      <div class="api-chain">
        <div class="api-step">
          <div class="step-name">前端页面</div>
          <div class="step-arrow">➡️</div>
        </div>
        <div class="api-step">
          <div class="step-name">Nginx</div>
          <div class="step-arrow">➡️</div>
        </div>
        <div class="api-step"
             :class="{ 'error': apiStatus.java === 'error' }">
          <div class="step-name">Java后端</div>
          <div class="step-time">{{ apiStatus.javaTime }}ms</div>
          <div class="step-arrow">➡️</div>
        </div>
        <div class="api-step"
             :class="{ 'error': apiStatus.python === 'error' }">
          <div class="step-name">Python采集</div>
          <div class="step-time">{{ apiStatus.pythonTime }}ms</div>
        </div>
      </div>
      <div class="api-status-detail">
        <p>Java -> Python 内部调用: {{ apiStatus.internalCall }}</p>
      </div>
    </section>
    
    <!-- 原始数据查询 -->
    <section class="monitor-section">
      <h2>📋 原始数据查询</h2>
      <div class="query-form">
        <input 
          v-model="queryFundCode" 
          placeholder="输入基金代码，如：011452"
          class="query-input"
          @keyup.enter="queryRawData"
        />
        <button @click="queryRawData" class="query-btn">查询</button>
      </div>
      
      <div v-if="rawData" class="raw-data">
        <h3>基金：{{ rawData.fundName }} ({{ rawData.fundCode }})</h3>
        <div class="data-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            :class="{ active: currentTab === tab.key }"
            @click="currentTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
        
        <pre class="data-content">{{ JSON.stringify(rawData[currentTab], null, 2) }}</pre>
      </div>
    </section>
    
    <!-- 实时日志 -->
    <section class="monitor-section">
      <h2>📝 最近操作日志</h2>
      <div class="log-list">
        <div v-for="(log, index) in logs" :key="index" class="log-item"
             :class="log.type">
          <span class="log-time">{{ log.time }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

const services = ref([
  { name: 'MySQL', status: 'unknown', responseTime: '-' },
  { name: 'Redis', status: 'unknown', responseTime: '-' },
  { name: 'Python采集', status: 'unknown', responseTime: '-' },
  { name: 'Java后端', status: 'unknown', responseTime: '-' },
  { name: 'Nginx前端', status: 'unknown', responseTime: '-' },
])

const dataStats = ref({
  totalFunds: 0,
  fundInfoRate: 0,
  metricsRate: 0,
  navRate: 0
})

const apiStatus = ref({
  java: 'unknown',
  python: 'unknown',
  javaTime: '-',
  pythonTime: '-',
  internalCall: 'unknown'
})

const queryFundCode = ref('011452')
const rawData = ref(null)
const currentTab = ref('info')

const tabs = [
  { key: 'info', label: '基本信息' },
  { key: 'metrics', label: '指标数据' },
  { key: 'nav', label: 'NAV历史' },
  { key: 'estimate', label: '实时估值' }
]

const logs = ref([])

let refreshTimer = null

const checkServices = async () => {
  // 检查Python服务
  try {
    const start = Date.now()
    const res = await axios.get('/api/collect/health')
    services.value[2].status = 'healthy'
    services.value[2].responseTime = (Date.now() - start) + 'ms'
  } catch (e) {
    services.value[2].status = 'error'
    services.value[2].responseTime = 'timeout'
  }
  
  // 检查Java服务
  try {
    const start = Date.now()
    const res = await axios.get('/actuator/health')
    services.value[3].status = 'healthy'
    services.value[3].responseTime = (Date.now() - start) + 'ms'
  } catch (e) {
    services.value[3].status = 'error'
  }
  
  // 检查Nginx（通过当前页面）
  services.value[4].status = 'healthy'
  services.value[4].responseTime = '0ms'
}

const fetchDataStats = async () => {
  try {
    // 获取基金列表
    const res = await axios.get('/api/funds?page=1&size=1')
    dataStats.value.totalFunds = res.data.data.total || 0
    
    // 这里简化处理，实际应该调用统计接口
    dataStats.value.fundInfoRate = 45  // 示例数据
    dataStats.value.metricsRate = 62
    dataStats.value.navRate = 38
  } catch (e) {
    console.error('获取数据统计失败', e)
  }
}

const checkApiChain = async () => {
  // 检查Java -> Python调用
  try {
    const start = Date.now()
    const res = await axios.get('/api/funds/011452/estimate')
    apiStatus.value.javaTime = (Date.now() - start) + 'ms'
    apiStatus.value.java = res.data.success ? 'ok' : 'error'
    apiStatus.value.internalCall = res.data.data ? '✅ 正常' : '❌ 失败'
  } catch (e) {
    apiStatus.value.java = 'error'
    apiStatus.value.internalCall = '❌ 异常: ' + e.message
  }
}

const queryRawData = async () => {
  const code = queryFundCode.value
  if (!code) return
  
  addLog('info', `开始查询基金 ${code} 的原始数据`)
  
  try {
    // 并行查询所有数据
    const [infoRes, metricsRes, navRes, estimateRes] = await Promise.all([
      axios.get(`/api/funds/${code}`).catch(() => ({ data: { data: {} } })),
      axios.get(`/api/funds/${code}/metrics`).catch(() => ({ data: { data: {} } })),
      axios.get(`/api/funds/${code}/nav/recent`).catch(() => ({ data: { data: [] } })),
      axios.get(`/api/funds/${code}/estimate`).catch(() => ({ data: { data: {} } }))
    ])
    
    rawData.value = {
      fundCode: code,
      fundName: infoRes.data.data?.fundName || code,
      info: infoRes.data.data,
      metrics: metricsRes.data.data,
      nav: navRes.data.data,
      estimate: estimateRes.data.data
    }
    
    // 检查数据缺失情况
    const missing = []
    if (!infoRes.data.data?.managerName) missing.push('基金经理')
    if (!infoRes.data.data?.companyName) missing.push('基金公司')
    if (!metricsRes.data.data?.return1y) missing.push('年化收益')
    if (!navRes.data.data?.length) missing.push('NAV历史')
    
    if (missing.length) {
      addLog('warning', `基金 ${code} 缺失数据: ${missing.join(', ')}`)
    } else {
      addLog('success', `基金 ${code} 数据完整`)
    }
  } catch (e) {
    addLog('error', `查询失败: ${e.message}`)
  }
}

const addLog = (type, message) => {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  logs.value.unshift({ type, time, message })
  if (logs.value.length > 20) logs.value.pop()
}

onMounted(() => {
  checkServices()
  fetchDataStats()
  checkApiChain()
  
  refreshTimer = setInterval(() => {
    checkServices()
    checkApiChain()
  }, 30000) // 每30秒刷新
  
  // 自动查询一次示例数据
  setTimeout(queryRawData, 500)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.monitor-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #0f1419;
}

.monitor-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

h2 {
  margin-bottom: 20px;
  color: #0f1419;
  font-size: 18px;
}

/* 服务状态网格 */
.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.service-card {
  padding: 16px;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s;
}

.service-card.healthy {
  background: rgba(0, 186, 124, 0.1);
  border: 1px solid rgba(0, 186, 124, 0.3);
}

.service-card.error {
  background: rgba(244, 33, 46, 0.1);
  border: 1px solid rgba(244, 33, 46, 0.3);
}

.service-card.unknown {
  background: #f7f9fa;
  border: 1px solid #eff3f4;
}

.service-name {
  font-weight: 600;
  margin-bottom: 8px;
}

.service-status {
  font-size: 14px;
  margin-bottom: 4px;
}

.service-time {
  font-size: 12px;
  color: #536471;
}

/* 数据统计 */
.data-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  border-radius: 12px;
  background: #f7f9fa;
  text-align: center;
  transition: all 0.3s;
}

.stat-card.warning {
  background: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
}

.stat-label {
  font-size: 14px;
  color: #536471;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #00acee;
}

/* API链路 */
.api-chain {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
  padding: 20px;
  background: #f7f9fa;
  border-radius: 12px;
}

.api-step {
  text-align: center;
  padding: 12px 20px;
  background: white;
  border-radius: 8px;
  min-width: 100px;
  position: relative;
}

.api-step.error {
  background: rgba(244, 33, 46, 0.1);
  border: 1px solid rgba(244, 33, 46, 0.3);
}

.step-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.step-time {
  font-size: 12px;
  color: #536471;
}

.step-arrow {
  font-size: 20px;
}

.api-status-detail {
  text-align: center;
  margin-top: 16px;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

/* 原始数据查询 */
.query-form {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.query-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #eff3f4;
  border-radius: 9999px;
  font-size: 16px;
  outline: none;
}

.query-input:focus {
  border-color: #00acee;
}

.query-btn {
  padding: 12px 24px;
  background: #00acee;
  color: white;
  border: none;
  border-radius: 9999px;
  font-weight: 600;
  cursor: pointer;
}

.query-btn:hover {
  background: #0095d1;
}

.data-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.data-tabs button {
  padding: 8px 16px;
  border: 1px solid #eff3f4;
  background: white;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.3s;
}

.data-tabs button.active {
  background: #00acee;
  color: white;
  border-color: #00acee;
}

.data-content {
  background: #f7f9fa;
  padding: 16px;
  border-radius: 12px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}

/* 日志 */
.log-list {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  padding: 10px 12px;
  border-bottom: 1px solid #eff3f4;
  display: flex;
  gap: 12px;
  font-size: 14px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #536471;
  font-family: monospace;
  min-width: 70px;
}

.log-item.info .log-message {
  color: #0f1419;
}

.log-item.success .log-message {
  color: #00ba7c;
}

.log-item.warning .log-message {
  color: #f59e0b;
}

.log-item.error .log-message {
  color: #f4212e;
}
</style>
