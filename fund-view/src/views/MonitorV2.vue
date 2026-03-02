<template>
  <div class="monitor-v2">
    <h1>🔍 链路追踪与数据透视面板</h1>
    
    <!-- 统计仪表盘 -->
    <section class="monitor-section">
      <h2>📊 链路统计（最近1小时）</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ traceStats.total }}</div>
          <div class="stat-label">总调用次数</div>
        </div>
        <div class="stat-card" :class="{ 'good': traceStats.successRate >= 80, 'bad': traceStats.successRate < 50 }">
          <div class="stat-value">{{ traceStats.successRate }}%</div>
          <div class="stat-label">成功率</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ traceStats.avgDuration }}ms</div>
          <div class="stat-label">平均耗时</div>
        </div>
        <div class="stat-card" :class="{ 'bad': traceStats.failed > 0 }">
          <div class="stat-value">{{ traceStats.failed }}</div>
          <div class="stat-label">失败次数</div>
        </div>
      </div>
    </section>

    <!-- 实时链路流 -->
    <section class="monitor-section">
      <h2>🌊 实时链路流（最近50条）</h2>
      <div class="trace-list">
        <div v-for="trace in traces" :key="trace.requestId" 
             class="trace-item" :class="trace.status">
          <div class="trace-header">
            <span class="trace-time">{{ formatTime(trace.startTime) }}</span>
            <span class="trace-code">{{ trace.fundCode }}</span>
            <span class="trace-type">{{ trace.apiType }}</span>
            <span class="trace-status" :class="trace.status">
              {{ trace.status === 'success' ? '✅' : '❌' }}
            </span>
            <span class="trace-duration">{{ trace.duration }}ms</span>
          </div>
          <div class="trace-detail" v-if="trace.status !== 'success'">
            <div class="error-msg">{{ trace.errorMessage }}</div>
          </div>
          <div class="trace-detail" v-else>
            <div class="detail-row">
              <span class="label">Python接口:</span>
              <span class="value">{{ trace.pythonUrl }}</span>
            </div>
            <div class="detail-row">
              <span class="label">HTTP状态:</span>
              <span class="value">{{ trace.pythonStatus }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 原始数据查询 -->
    <section class="monitor-section">
      <h2>🔬 原始数据透视（绕过Java处理）</h2>
      <div class="query-section">
        <div class="query-row">
          <input v-model="rawQuery.fundCode" placeholder="基金代码，如：011452" class="query-input" />
          <select v-model="rawQuery.type" class="query-select">
            <option value="info">基本信息</option>
            <option value="metrics">指标数据</option>
            <option value="nav">NAV历史</option>
            <option value="estimate">实时估值</option>
          </select>
          <button @click="queryRawPythonData" class="query-btn" :disabled="queryingRaw">
            {{ queryingRaw ? '查询中...' : '查询Python原始数据' }}
          </button>
        </div>
        
        <div v-if="rawPythonData" class="raw-data-panel">
          <h3>Python服务原始返回</h3>
          <div class="data-meta">
            <span :class="['status-badge', rawPythonData.success ? 'success' : 'error']">
              {{ rawPythonData.success ? '成功' : '失败' }}
            </span>
            <span v-if="rawPythonData.error" class="error-text">{{ rawPythonData.error }}</span>
          </div>
          <pre class="json-viewer">{{ JSON.stringify(rawPythonData, null, 2) }}</pre>
        </div>
      </div>
    </section>

    <!-- 数据对比 -->
    <section class="monitor-section">
      <h2>⚖️ 数据对比：Python原始 vs Java处理后</h2>
      <div class="compare-section">
        <div class="query-row">
          <input v-model="compareCode" placeholder="基金代码，如：011452" class="query-input" />
          <button @click="compareData" class="query-btn" :disabled="comparing">
            {{ comparing ? '对比中...' : '执行对比' }}
          </button>
        </div>
        
        <div v-if="compareResult" class="compare-result">
          <div class="compare-panel">
            <h3>🐍 Python原始返回</h3>
            <div class="panel-content">
              <div v-if="compareResult.pythonRaw?.error" class="error-box">
                错误: {{ compareResult.pythonRaw.error }}
              </div>
              <pre v-else class="json-viewer">{{ JSON.stringify(compareResult.pythonRaw, null, 2) }}</pre>
            </div>
          </div>
          
          <div class="compare-arrow">➡️ Java处理 ➡️</div>
          
          <div class="compare-panel">
            <h3>☕ Java处理后</h3>
            <div class="panel-content">
              <div class="process-info">
                <div class="info-row">
                  <span class="label">处理成功:</span>
                  <span :class="['value', compareResult.javaProcessed?.success ? 'success' : 'error']">
                    {{ compareResult.javaProcessed?.success ? '✅' : '❌' }}
                  </span>
                </div>
                <div class="info-row" v-if="compareResult.javaProcessed?.errorCode">
                  <span class="label">错误码:</span>
                  <span class="value error">{{ compareResult.javaProcessed.errorCode }}</span>
                </div>
                <div class="info-row" v-if="compareResult.javaProcessed?.message">
                  <span class="label">消息:</span>
                  <span class="value">{{ compareResult.javaProcessed.message }}</span>
                </div>
                <div class="info-row">
                  <span class="label">数据:</span>
                  <span class="value">{{ compareResult.javaProcessed?.data || 'null' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="compareResult" class="analysis-box">
          <h4>🔍 分析结论</h4>
          <p v-if="compareResult.pythonRaw?.error && !compareResult.javaProcessed?.success">
            ⚠️ Python端返回错误，Java正常传递了错误信息
          </p>
          <p v-else-if="!compareResult.pythonRaw?.error && compareResult.javaProcessed?.success">
            ✅ Python返回成功，Java处理正常
          </p>
          <p v-else-if="compareResult.pythonRaw?.success && !compareResult.javaProcessed?.success">
            ❌ Python返回成功，但Java处理失败（数据处理逻辑有问题）
          </p>
          <p v-else>
            🤔 需要进一步查看详情
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

// 链路追踪数据
const traces = ref([])
const traceStats = ref({
  total: 0,
  success: 0,
  failed: 0,
  successRate: 0,
  avgDuration: 0
})

// 原始数据查询
const rawQuery = ref({
  fundCode: '011452',
  type: 'info'
})
const queryingRaw = ref(false)
const rawPythonData = ref(null)

// 数据对比
const compareCode = ref('011452')
const comparing = ref(false)
const compareResult = ref(null)

let refreshTimer = null

// 获取链路追踪
const fetchTraces = async () => {
  try {
    const res = await axios.get('/api/monitor/traces?limit=50')
    if (res.data.success) {
      traces.value = res.data.data.traces || []
      if (res.data.data.stats) {
        traceStats.value = res.data.data.stats
      }
    }
  } catch (e) {
    console.error('获取链路追踪失败', e)
  }
}

// 查询Python原始数据
const queryRawPythonData = async () => {
  const { fundCode, type } = rawQuery.value
  if (!fundCode) return
  
  queryingRaw.value = true
  rawPythonData.value = null
  
  try {
    const res = await axios.get(`/api/monitor/raw/python/${fundCode}?type=${type}`)
    rawPythonData.value = res.data
  } catch (e) {
    rawPythonData.value = { 
      success: false, 
      error: e.message,
      note: '请求失败，请检查Python服务是否正常'
    }
  } finally {
    queryingRaw.value = false
  }
}

// 对比数据
const compareData = async () => {
  const code = compareCode.value
  if (!code) return
  
  comparing.value = true
  compareResult.value = null
  
  try {
    const res = await axios.get(`/api/monitor/compare/${code}`)
    if (res.data.success) {
      compareResult.value = res.data.data
    }
  } catch (e) {
    compareResult.value = {
      error: e.message
    }
  } finally {
    comparing.value = false
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit'
  })
}

onMounted(() => {
  fetchTraces()
  refreshTimer = setInterval(fetchTraces, 10000) // 每10秒刷新
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.monitor-v2 {
  max-width: 1400px;
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
  font-size: 18px;
  color: #0f1419;
}

/* 统计仪表盘 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: #f7f9fa;
  border-radius: 12px;
  transition: all 0.3s;
}

.stat-card.good {
  background: rgba(0, 186, 124, 0.1);
  border: 1px solid rgba(0, 186, 124, 0.3);
}

.stat-card.bad {
  background: rgba(244, 33, 46, 0.1);
  border: 1px solid rgba(244, 33, 46, 0.3);
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #00acee;
}

.stat-label {
  margin-top: 8px;
  color: #536471;
  font-size: 14px;
}

/* 链路追踪 */
.trace-list {
  max-height: 500px;
  overflow-y: auto;
}

.trace-item {
  padding: 16px;
  border-bottom: 1px solid #eff3f4;
  transition: background 0.2s;
}

.trace-item:hover {
  background: #f7f9fa;
}

.trace-item.error {
  background: rgba(244, 33, 46, 0.05);
}

.trace-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.trace-time {
  font-family: monospace;
  color: #536471;
  font-size: 14px;
}

.trace-code {
  font-weight: 600;
  color: #00acee;
}

.trace-type {
  padding: 2px 8px;
  background: #e8f5fe;
  border-radius: 4px;
  font-size: 12px;
  color: #00acee;
}

.trace-status {
  font-size: 16px;
}

.trace-duration {
  margin-left: auto;
  color: #536471;
  font-size: 14px;
}

.trace-detail {
  margin-top: 8px;
  padding-left: 16px;
  font-size: 13px;
}

.error-msg {
  color: #f4212e;
}

.detail-row {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.detail-row .label {
  color: #536471;
  min-width: 80px;
}

/* 查询区域 */
.query-section, .compare-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.query-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.query-input {
  flex: 1;
  min-width: 150px;
  padding: 12px 16px;
  border: 1px solid #eff3f4;
  border-radius: 9999px;
  font-size: 16px;
  outline: none;
}

.query-input:focus {
  border-color: #00acee;
}

.query-select {
  padding: 12px 16px;
  border: 1px solid #eff3f4;
  border-radius: 9999px;
  font-size: 16px;
  background: white;
}

.query-btn {
  padding: 12px 24px;
  background: #00acee;
  color: white;
  border: none;
  border-radius: 9999px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.query-btn:hover:not(:disabled) {
  background: #0095d1;
}

.query-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 原始数据面板 */
.raw-data-panel, .compare-panel {
  background: #f7f9fa;
  border-radius: 12px;
  padding: 20px;
  margin-top: 16px;
}

.data-meta {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.success {
  background: rgba(0, 186, 124, 0.1);
  color: #00ba7c;
}

.status-badge.error {
  background: rgba(244, 33, 46, 0.1);
  color: #f4212e;
}

.error-text {
  color: #f4212e;
}

.json-viewer {
  background: #1a1a2e;
  color: #eaeaea;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 400px;
  overflow-y: auto;
}

/* 对比区域 */
.compare-result {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 1024px) {
  .compare-result {
    grid-template-columns: 1fr;
  }
  .compare-arrow {
    transform: rotate(90deg);
    text-align: center;
  }
}

.compare-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #536471;
  padding: 20px;
}

.compare-panel h3 {
  margin-bottom: 12px;
  font-size: 16px;
}

.process-info {
  background: white;
  padding: 16px;
  border-radius: 8px;
}

.info-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.info-row .label {
  color: #536471;
  min-width: 80px;
}

.info-row .value {
  font-weight: 500;
}

.info-row .value.success {
  color: #00ba7c;
}

.info-row .value.error {
  color: #f4212e;
}

.error-box {
  background: rgba(244, 33, 46, 0.1);
  color: #f4212e;
  padding: 12px;
  border-radius: 8px;
}

/* 分析框 */
.analysis-box {
  background: #e8f5fe;
  border-left: 4px solid #00acee;
  padding: 16px;
  border-radius: 0 8px 8px 0;
  margin-top: 16px;
}

.analysis-box h4 {
  margin-bottom: 8px;
  color: #0f1419;
}

.analysis-box p {
  color: #536471;
  line-height: 1.6;
}
</style>
