<template>
  <div class="portfolio-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">持仓管理</h1>
      <button class="btn-primary" @click="showTradeDialog = true">
        + 记录交易
      </button>
    </div>
    
    <!-- 组合概览卡片 -->
    <div class="overview-cards">
      <div class="overview-card">
        <div class="overview-icon">
          <el-icon :size="32"><Money /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-label">总市值</div>
          <div class="overview-value">{{ formatMoney(analysis.totalValue) }}</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="overview-icon">
          <el-icon :size="32"><DataLine /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-label">总成本</div>
          <div class="overview-value">{{ formatMoney(analysis.totalCost) }}</div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="overview-icon">
          <el-icon :size="32"><TrendCharts /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-label">总收益</div>
          <div class="overview-value" :class="analysis.totalReturn >= 0 ? 'positive' : 'negative'">
            {{ formatMoney(analysis.totalReturn) }}
          </div>
        </div>
      </div>
      
      <div class="overview-card">
        <div class="overview-icon">
          <el-icon :size="32"><Aim /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-label">收益率</div>
          <div class="overview-value" :class="analysis.totalReturnRate >= 0 ? 'positive' : 'negative'">
            {{ formatPercent(analysis.totalReturnRate) }}
          </div>
        </div>
      </div>
    </div>
    
    <!-- 持仓列表 -->
    <div class="holdings-section" v-loading="loading">
      <div class="section-header">
        <h2 class="section-title">持仓明细 ({{ holdings.length }}只基金)</h2>
      </div>
      
      <div v-if="holdings.length > 0" class="holdings-list">
        <div
          v-for="holding in holdings"
          :key="holding.fundCode"
          class="holding-item"
          @click="viewDetail(holding)"
        >
          <div class="holding-main">
            <div class="holding-info">
              <div class="holding-name">{{ holding.fundName }}</div>
              <div class="holding-code">{{ holding.fundCode }}</div>
            </div>
            
            <div class="holding-tags">
              <span v-if="holding.qualityLevel" class="tag" :class="'tag-' + getQualityColor(holding.qualityLevel)">
                {{ holding.qualityLevel }}级
              </span>
            </div>
          </div>
          
          <div class="holding-metrics">
            <div class="metric">
              <div class="metric-label">持有份额</div>
              <div class="metric-value">{{ formatNumber(holding.totalShares) }}</div>
            </div>
            
            <div class="metric">
              <div class="metric-label">平均成本</div>
              <div class="metric-value">{{ formatNumber(holding.avgCost, 4) }}</div>
            </div>
            
            <div class="metric">
              <div class="metric-label">当前市值</div>
              <div class="metric-value">{{ formatMoney(holding.currentValue) }}</div>
            </div>
            
            <div class="metric">
              <div class="metric-label">收益</div>
              <div class="metric-value" :class="holding.totalReturn >= 0 ? 'positive' : 'negative'">
                {{ formatMoney(holding.totalReturn) }}
              </div>
            </div>
            
            <div class="metric">
              <div class="metric-label">收益率</div>
              <div class="metric-value" :class="holding.returnRate >= 0 ? 'positive' : 'negative'">
                {{ formatPercent(holding.returnRate) }}
              </div>
            </div>
          </div>
          
          <div class="holding-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">
          <el-icon :size="64"><Wallet /></el-icon>
        </div>
        <div class="empty-title">暂无持仓</div>
        <div class="empty-subtitle">点击下方按钮记录您的第一笔交易</div>
        
        <button class="btn-primary" @click="showTradeDialog = true">
          记录交易
        </button>
      </div>
    </div>
    
    <!-- 质量分布 -->
    <div v-if="Object.keys(analysis.qualityDistribution || {}).length > 0" class="distribution-section">
      <div class="section-title">持仓质量分布</div>
      
      <div class="distribution-grid">
        <div
          v-for="(count, level) in analysis.qualityDistribution"
          :key="level"
          class="distribution-item"
        >
          <div class="distribution-bar">
            <div
              class="distribution-fill"
              :class="'fill-' + getQualityColor(level)"
              :style="{ width: (count / holdings.length * 100) + '%' }"
            ></div>
          </div>          
          <div class="distribution-info">
            <span class="distribution-level">{{ level }}级</span>
            <span class="distribution-count">{{ count }}只</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 记录交易弹窗 -->
    <div v-if="showTradeDialog" class="dialog-overlay" @click.self="showTradeDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>记录交易</h3>
          <button class="dialog-close" @click="showTradeDialog = false">×</button>
        </div>        
        <div class="dialog-body">
          <div class="form-group">
            <label>基金代码</label>
            <input v-model="tradeForm.fundCode" type="text" class="form-input" placeholder="如: 000001" />
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>交易日期</label>
              <input v-model="tradeForm.tradeDate" type="date" class="form-input" />
            </div>            
            <div class="form-group">
              <label>交易类型</label>
              <select v-model="tradeForm.tradeType" class="form-input">
                <option :value="1">买入</option>
                <option :value="2">卖出</option>
              </select>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>交易份额</label>
              <input v-model="tradeForm.tradeShare" type="number" class="form-input" placeholder="0.00" step="0.01" />
            </div>            
            <div class="form-group">
              <label>交易净值</label>
              <input v-model="tradeForm.tradePrice" type="number" class="form-input" placeholder="0.0000" step="0.0001" />
            </div>
          </div>
          
          <div class="form-group">
            <label>手续费</label>
            <input v-model="tradeForm.tradeFee" type="number" class="form-input" placeholder="0.00" step="0.01" />
          </div>
          
          <div class="form-group">
            <label>备注</label>
            <textarea v-model="tradeForm.remark" class="form-input" rows="2" placeholder="选填"></textarea>
          </div>
        </div>        
        <div class="dialog-footer">
          <button class="btn-outline" @click="showTradeDialog = false">取消</button>
          <button class="btn-primary" @click="submitTrade">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { portfolioApi } from '@/api'
import { formatNumber, formatPercent } from '@/utils'

const router = useRouter()
const loading = ref(false)
const holdings = ref([])
const analysis = ref({})
const showTradeDialog = ref(false)

const tradeForm = reactive({
  fundCode: '',
  tradeDate: new Date().toISOString().split('T')[0],
  tradeType: 1,
  tradeShare: 0,
  tradePrice: 0,
  tradeFee: 0,
  remark: '',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const [holdingsRes, analysisRes] = await Promise.all([
      portfolioApi.getHoldings(),
      portfolioApi.getAnalysis(),
    ])
    
    if (holdingsRes.success) holdings.value = holdingsRes.data
    if (analysisRes.success) analysis.value = analysisRes.data
  } finally {
    loading.value = false
  }
}

// 提交交易
const submitTrade = async () => {
  try {
    const res = await portfolioApi.recordTrade({
      ...tradeForm,
      tradeShare: Number(tradeForm.tradeShare),
      tradePrice: Number(tradeForm.tradePrice),
      tradeFee: Number(tradeForm.tradeFee),
    })
    
    if (res.success) {
      ElMessage.success('交易记录成功')
      showTradeDialog.value = false
      fetchData()
      // 重置表单
      Object.assign(tradeForm, {
        fundCode: '',
        tradeDate: new Date().toISOString().split('T')[0],
        tradeType: 1,
        tradeShare: 0,
        tradePrice: 0,
        tradeFee: 0,
        remark: '',
      })
    }
  } catch (error) {
    ElMessage.error('记录失败')
  }
}

// 查看详情
const viewDetail = (holding) => {
  router.push(`/fund/${holding.fundCode}`)
}

// 工具函数
const formatMoney = (val) => {
  if (val === undefined || val === null) return '¥0.00'
  const num = Number(val)
  return (num >= 0 ? '¥' : '-¥') + Math.abs(num).toFixed(2)
}

const getQualityColor = (level) => {
  const colors = { S: 'danger', A: 'success', B: 'primary', C: 'warning', D: 'info' }
  return colors[level] || 'primary'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.portfolio-page {
  max-width: 900px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
}

/* 概览卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.overview-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

.overview-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.overview-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.overview-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
}

.overview-value.positive {
  color: #00ba7c;
}

.overview-value.negative {
  color: #f4212e;
}

/* 持仓区域 */
.holdings-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.section-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.section-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
}

/* 持仓列表 */
.holdings-list {
  padding: 8px 0;
}

.holding-item {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition);
}

.holding-item:last-child {
  border-bottom: none;
}

.holding-item:hover {
  background: var(--bg-hover);
}

.holding-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.holding-info {
  min-width: 0;
}

.holding-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.holding-code {
  font-size: 13px;
  color: var(--text-secondary);
}

.holding-tags {
  flex-shrink: 0;
}

.holding-metrics {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.metric {
  text-align: center;
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.metric-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-value.positive {
  color: #00ba7c;
}

.metric-value.negative {
  color: #f4212e;
}

.holding-arrow {
  display: none;
  text-align: right;
  color: var(--text-secondary);
  margin-top: 12px;
}

.holding-item:hover .holding-arrow {
  display: block;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 分布区域 */
.distribution-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
}

.distribution-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.distribution-item {
  text-align: center;
}

.distribution-bar {
  height: 8px;
  background: var(--bg-secondary);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.distribution-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.fill-danger { background: linear-gradient(90deg, #ff6b6b, #ee5a5a); }
.fill-success { background: linear-gradient(90deg, #00ba7c, #00a870); }
.fill-primary { background: linear-gradient(90deg, #00acee, #0095d1); }
.fill-warning { background: linear-gradient(90deg, #ffb347, #ffa500); }
.fill-info { background: linear-gradient(90deg, #74b9ff, #5fa8ff); }

.distribution-level {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  display: block;
  margin-bottom: 4px;
}

.distribution-count {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 弹窗 */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.dialog {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.dialog-header h3 {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
}

.dialog-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 50%;
  transition: var(--transition);
}

.dialog-close:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.dialog-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

/* 表单 */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 15px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  outline: none;
  transition: var(--transition);
}

.form-input:focus {
  border-color: var(--primary-color);
  background: var(--bg-primary);
  box-shadow: 0 0 0 3px rgba(0, 172, 238, 0.15);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .holding-metrics {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
  
  .distribution-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
