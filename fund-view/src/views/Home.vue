<template>
  <div class="home">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-card">
        <div class="welcome-content">
          <h1 class="welcome-title">发现优质基金</h1>
          <p class="welcome-subtitle">智能分析，科学决策，让投资更简单</p>
          <div class="welcome-actions">
            <button class="btn-primary" @click="$router.push('/funds')">
              <el-icon><Search /></el-icon> 开始探索
            </button>
            <button class="btn-outline" @click="$router.push('/portfolio')">
              <el-icon><Wallet /></el-icon> 管理持仓
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon">
          <el-icon :size="32">
            <component :is="stat.icon" />
          </el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>
    
    <!-- TOP基金 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon :size="20" style="margin-right: 8px;"><Trophy /></el-icon>
          TOP 10 基金
        </h2>
        <router-link to="/funds" class="section-link">查看全部 →</router-link>
      </div>
      
      <div class="fund-list" v-loading="loading">
        <div 
          v-for="(fund, index) in topFunds" 
          :key="fund.fundCode"
          class="fund-item"
          @click="viewDetail(fund)"
        >
          <div class="fund-rank">{{ index + 1 }}</div>
          
          <div class="fund-info">
            <div class="fund-name">{{ fund.fundName }}</div>
            <div class="fund-code">{{ fund.fundCode }}</div>
          </div>
          
          <div class="fund-metrics">
            <div 
              class="metric-sharpe"
              :class="{ 'high': fund.sharpeRatio1y >= 2 }"
            >
              夏普 {{ fund.sharpeRatio1y?.toFixed(2) }}
            </div>
            
            <div 
              class="metric-return"
              :class="fund.return1y > 0 ? 'positive' : 'negative'"
            >
              {{ fund.return1y > 0 ? '+' : '' }}{{ fund.return1y?.toFixed(2) }}%
            </div>
          </div>
          
          <div class="fund-arrow">→</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fundApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const topFunds = ref([])

const stats = [
  { icon: 'DataLine', value: '26,180', label: '基金数量' },
  { icon: 'Star', value: '1000+', label: '评级覆盖' },
  { icon: 'Cpu', value: 'AI', label: '智能信号' },
  { icon: 'Money', value: '实时', label: '收益追踪' },
]

onMounted(async () => {
  loading.value = true
  try {
    const res = await fundApi.getTopFunds('sharpe', 10)
    if (res.success) {
      topFunds.value = res.data
    }
  } finally {
    loading.value = false
  }
})

const viewDetail = (fund) => {
  router.push(`/fund/${fund.fundCode}`)
}
</script>

<style scoped>
.home {
  max-width: 900px;
  margin: 0 auto;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: 32px;
}

.welcome-card {
  background: linear-gradient(135deg, #00acee 0%, #1d9bf0 100%);
  border-radius: var(--radius-lg);
  padding: 48px 40px;
  color: white;
  text-align: center;
  box-shadow: var(--shadow-md);
  position: relative;
  overflow: hidden;
}

.welcome-card::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 60%);
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 12px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.welcome-subtitle {
  font-size: 18px;
  opacity: 0.95;
  margin-bottom: 32px;
}

.welcome-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.welcome-actions .btn-primary {
  background: white;
  color: var(--primary-color);
  padding: 14px 32px;
  font-size: 16px;
}

.welcome-actions .btn-primary:hover {
  background: #f0f8ff;
  transform: scale(1.05);
}

.welcome-actions .btn-outline {
  border-color: rgba(255,255,255,0.8);
  color: white;
}

.welcome-actions .btn-outline::before {
  background: rgba(255,255,255,0.15);
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  padding: 24px 20px;
  text-align: center;
  border: 1px solid var(--border-color);
  transition: var(--transition);
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-color);
}

.stat-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 区域标题 */
.section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.section-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  display: flex;
  align-items: center;
}

.section-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.section-link:hover {
  background: var(--bg-hover);
}

/* 基金列表 */
.fund-list {
  padding: 8px 0;
}

.fund-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 1px solid var(--border-color);
}

.fund-item:last-child {
  border-bottom: none;
}

.fund-item:hover {
  background: var(--bg-hover);
}

.fund-rank {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  border-radius: 50%;
  font-weight: 700;
  font-size: 14px;
  color: var(--text-secondary);
}

.fund-item:nth-child(1) .fund-rank {
  background: linear-gradient(135deg, #ffd700 0%, #ffb800 100%);
  color: white;
}

.fund-item:nth-child(2) .fund-rank {
  background: linear-gradient(135deg, #c0c0c0 0%, #a0a0a0 100%);
  color: white;
}

.fund-item:nth-child(3) .fund-rank {
  background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%);
  color: white;
}

.fund-info {
  flex: 1;
  min-width: 0;
}

.fund-name {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fund-code {
  font-size: 13px;
  color: var(--text-secondary);
}

.fund-metrics {
  display: flex;
  gap: 12px;
  align-items: center;
}

.metric-sharpe {
  padding: 6px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.metric-sharpe.high {
  background: rgba(0, 172, 238, 0.1);
  color: var(--primary-color);
}

.metric-return {
  font-weight: 700;
  font-size: 14px;
  min-width: 70px;
  text-align: right;
}

.metric-return.positive {
  color: #00ba7c;
}

.metric-return.negative {
  color: #f4212e;
}

.fund-arrow {
  color: var(--text-secondary);
  opacity: 0;
  transition: var(--transition);
}

.fund-item:hover .fund-arrow {
  opacity: 1;
  transform: translateX(4px);
}

/* 响应式 */
@media (max-width: 768px) {
  .welcome-title {
    font-size: 28px;
  }
  
  .welcome-actions {
    flex-direction: column;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .fund-metrics {
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }
}
</style>
