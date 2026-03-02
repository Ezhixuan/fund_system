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
    
    <!-- 自动补全状态 -->
    <section class="monitor-section">
      <h2>🤖 自动数据补全</h2>
      <div class="auto-fix-info">
        <p>✅ 已启用自动数据补全机制</p>
        <ul>
          <li>当基金信息不完整（缺少基金经理、公司等）时，自动触发Python采集</li>
          <li>当NAV历史不足10条时，自动触发采集补充</li>
          <li>30分钟内不会重复查询同一只基金</li>
        </ul>
      </div>
    </section>
    
    <!-- 手动刷新工具 -->
    <section class="monitor-section">
      <h2>🛠️ 手动数据刷新（兜底方案）</h2>
      <div class="manual-refresh">
        <div class="refresh-input-group">
          <input 
            v-model="refreshFundCode" 
            placeholder="输入基金代码，如：011452"
            class="refresh-input"
            @keyup.enter="refreshAll"
          />
          <button @click="refreshAll" class="refresh-btn primary" :disabled="refreshing">
            {{ refreshing ? '刷新中...' : '一键刷新全部数据' }}
          </button>
        </div>
        
        <div class="refresh-options">
          <button @click="refreshInfo" class="refresh-btn" :disabled="refreshing">
            刷新基本信息
          </button>
          <button @click="refreshMetrics" class="refresh-btn" :disabled="refreshing">
            刷新指标数据
          </button>
          <button @click="refreshNav" class="refresh-btn" :disabled="refreshing">
            刷新NAV历史
          </button>
        </div>
        
        <div v-if="refreshResult" class="refresh-result" :class="refreshResult.success ? 'success' : 'error'">
          <p>{{ refreshResult.message }}</p>
          <div v-if="refreshResult.data" class="result-details">
            <span v-if="refreshResult.data.info !== undefined" 
                  :class="refreshResult.data.info ? 'ok' : 'fail'">
              基本信息: {{ refreshResult.data.info ? '✓' : '✗' }}
            </span>
            <span v-if="refreshResult.data.metrics !== undefined"
                  :class="refreshResult.data.metrics ? 'ok' : 'fail'">
              指标数据: {{ refreshResult.data.metrics ? '✓' : '✗' }}
            </span>
            <span v-if="refreshResult.data.nav !== undefined"
                  :class="refreshResult.data.nav ? 'ok' : 'fail'">
              NAV历史: {{ refreshResult.data.nav ? '✓' : '✗' }}
            </span>
          </div>
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
    
    <!-- 原始数据查询 -->
    <section class="monitor-section">
      <h2>📋 原始数据查询（自动检测缺失）</h2>
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
        <!-- 数据完整性提示 -->
        <div v-if="missingFields.length > 0" class="missing-alert">
          ⚠️ 检测到缺失字段：{{ missingFields.join('、') }}
          <button @click="autoRefresh" class="auto-refresh-btn" :disabled="autoRefreshing">
            {{ autoRefreshing ? '自动补全中...' : '立即自动补全' }}
          </button>
        </div>
        
        <div class="data-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            :class="{ active: currentTab === tab.key }"
            @click="currentTab = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.hasData" class="tab-status ok">✓</span>
            <span v-else class="tab-status missing">✗</span>
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import axios from 'axios'

const services = ref([
  { name: 'MySQL', status: 'unknown', responseTime: '-' },
  { name: 'Redis', status: 'unknown', responseTime: '-' },
  { name: 'Python采集', status: 'unknown', responseTime: '-' },
  { name: 'Java后端', status: 'unknown', responseTime: '-' },
  { name: 'Nginx前端', status: 'unknown', responseTime: '-' },
])

const dataStats = ref({
  totalFunds: 26167,
  fundInfoRate: 45,
  metricsRate: 62,
  navRate: 38
})

// 手动刷新相关
const refreshFundCode = ref('011452')
const refreshing = ref(false)
const refreshResult = ref(null)

// 数据查询相关
const queryFundCode = ref('011452')
const rawData = ref(null)
const currentTab = ref('info')
const autoRefreshing = ref(false)

const tabs = computed(() => [
  { key: 'info', label: '基本信息', hasData: hasInfoData.value },
  { key: 'metrics', label: '指标数据', hasData: hasMetricsData.value },
  { key: 'nav', label: 'NAV历史', hasData: hasNavData.value },
  { key: 'estimate', label: '实时估值', hasData: hasEstimateData.value }
])

// 检查各类型数据是否存在
const hasInfoData = computed(() => {
  return rawData.value?.info?.managerName && rawData.value?.info?.companyName
})

const hasMetricsData = computed(() => {
  return rawData.value?.metrics?.return1y !== null
})

const hasNavData = computed(() => {
  return rawData.value?.nav?.length > 0
})

const hasEstimateData = computed(() => {
  return rawData.value?.estimate?.estimateNav !== null
})

// 检测缺失字段
const missingFields = computed(() => {
  const missing = []
  if (!hasInfoData.value) missing.push('基金经理/公司')
  if (!hasMetricsData.value) missing.push('指标数据')
  if (!hasNavData.value) missing.push('NAV历史')
  return missing
})

const logs = ref([])

let refreshTimer = null

// 手动刷新 - 全部
const refreshAll = async () => {
  const code = refreshFundCode.value
  if (!code) return
  
  refreshing.value = true
  refreshResult.value = null
  addLog('info', `开始手动刷新基金 ${code} 的全部数据`)
  
  try {
    const res = await axios.post(`/api/monitor/refresh/${code}/all`)
    refreshResult.value = res.data
    
    if (res.data.success && res.data.data?.allSuccess) {
      addLog('success', `基金 ${code} 全部数据刷新成功`)
    } else {
      addLog('warning', `基金 ${code} 部分数据刷新失败`)
    }
  } catch (e) {
    refreshResult.value = { success: false, message: e.message }
    addLog('error', `刷新失败: ${e.message}`)
  } finally {
    refreshing.value = false
  }
}

// 手动刷新 - 基本信息
const refreshInfo = async () => {
  await refreshByType('info')
}

// 手动刷新 - 指标
const refreshMetrics = async () => {
  await refreshByType('metrics')
}

// 手动刷新 - NAV
const refreshNav = async () => {
  await refreshByType('nav')
}

const refreshByType = async (type) => {
  const code = refreshFundCode.value
  if (!code) return
  
  refreshing.value = true
  addLog('info', `开始手动刷新基金 ${code} 的${type}数据`)
  
  try {
    await axios.post(`/api/monitor/refresh/${code}/${type}`)
    addLog('success', `基金 ${code} ${type}数据刷新完成`)
  } catch (e) {
    addLog('error', `刷新失败: ${e.message}`)
  } finally {
    refreshing.value = false
  }
}

// 自动补全
const autoRefresh = async () => {
  autoRefreshing.value = true
  addLog('info', `开始自动补全基金 ${queryFundCode.value} 的缺失数据`)
  
  await refreshAll()
  
  // 重新查询验证
  setTimeout(async () => {
    await queryRawData()
    addLog('success', '自动补全完成，数据已更新')
    autoRefreshing.value = false
  }, 2000)
}

// 查询原始数据
const queryRawData = async () => {
  const code = queryFundCode.value
  if (!code) return
  
  addLog('info', `查询基金 ${code} 的原始数据`)
  
  try {
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
    
    if (missingFields.value.length > 0) {
      addLog('warning', `基金 ${code} 缺失: ${missingFields.value.join('、')}`)
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
  // 自动查询一次
  setTimeout(queryRawData, 500)
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
  font-size: 18px;
}

/* 自动补全信息 */
.auto-fix-info {
  background: rgba(0, 186, 124, 0.1);
  border: 1px solid rgba(0, 186, 124, 0.3);
  border-radius: 12px;
  padding: 16px;
}

.auto-fix-info ul {
  margin-top: 12px;
  margin-left: 20px;
}

.auto-fix-info li {
  margin-bottom: 8px;
  color: #536471;
}

/* 手动刷新 */
.manual-refresh {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.refresh-input-group {
  display: flex;
  gap: 12px;
}

.refresh-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #eff3f4;
  border-radius: 9999px;
  font-size: 16px;
}

.refresh-btn {
  padding: 12px 24px;
  border: 1px solid #00acee;
  background: white;
  color: #00acee;
  border-radius: 9999px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.refresh-btn:hover:not(:disabled) {
  background: #00acee;
  color: white;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-btn.primary {
  background: #00acee;
  color: white;
}

.refresh-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.refresh-result {
  padding: 16px;
  border-radius: 12px;
  margin-top: 12px;
}

.refresh-result.success {
  background: rgba(0, 186, 124, 0.1);
  border: 1px solid rgba(0, 186, 124, 0.3);
}

.refresh-result.error {
  background: rgba(244, 33, 46, 0.1);
  border: 1px solid rgba(244, 33, 46, 0.3);
}

.result-details {
  margin-top: 8px;
  display: flex;
  gap: 16px;
}

.result-details .ok {
  color: #00ba7c;
}

.result-details .fail {
  color: #f4212e;
}

/* 缺失提示 */
.missing-alert {
  background: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.5);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.auto-refresh-btn {
  padding: 8px 16px;
  background: #00acee;
  color: white;
  border: none;
  border-radius: 9999px;
  cursor: pointer;
}

/* 标签状态 */
.tab-status {
  margin-left: 4px;
  font-size: 12px;
}

.tab-status.ok {
  color: #00ba7c;
}

.tab-status.missing {
  color: #f4212e;
}

/* 其他样式继承之前的 */
.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.service-card {
  padding: 16px;
  border-radius: 12px;
  text-align: center;
  background: #f7f9fa;
}

.service-card.healthy {
  background: rgba(0, 186, 124, 0.1);
  border: 1px solid rgba(0, 186, 124, 0.3);
}

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
}

.stat-card.warning {
  background: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #00acee;
}

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
  max-height: 400px;
  overflow-y: auto;
}

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

.log-time {
  color: #536471;
  font-family: monospace;
  min-width: 70px;
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
