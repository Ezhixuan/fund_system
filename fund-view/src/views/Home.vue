<template>
  <div class="home">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <el-card>
        <template #header>
          <div class="welcome-header">
            <h1>欢迎使用基金智选系统</h1>
            <p>智能分析，科学决策，让投资更简单</p>
          </div>
        </template>
        <div class="quick-stats">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">📊</div>
                <div class="stat-info">
                  <div class="stat-value">26,180</div>
                  <div class="stat-label">基金数量</div>
                </div>
              </div>
            </el-col>
            
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">⭐</div>
                <div class="stat-info">
                  <div class="stat-value">S级</div>
                  <div class="stat-label">优质基金</div>
                </div>
              </div>
            </el-col>
            
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">📈</div>
                <div class="stat-info">
                  <div class="stat-value">AI</div>
                  <div class="stat-label">智能信号</div>
                </div>
              </div>
            </el-col>
            
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">💼</div>
                <div class="stat-info">
                  <div class="stat-value">持仓</div>
                  <div class="stat-label">收益追踪</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>
    
    <!-- 快捷入口 -->
    <div class="quick-links">
      <h2>快捷入口</h2>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="link-card" @click="$router.push('/funds')">
            <div class="link-icon">🔍</div>
            <div class="link-title">基金搜索</div>
            <div class="link-desc">26,000+基金随心搜</div>
          </el-card>
        </el-col>
        
        <el-col :span="8">
          <el-card class="link-card" @click="$router.push('/portfolio')">
            <div class="link-icon">💰</div>
            <div class="link-title">持仓管理</div>
            <div class="link-desc">追踪收益，智能分析</div>
          </el-card>
        </el-col>
        
        <el-col :span="8">
          <el-card class="link-card">
            <div class="link-icon">📊</div>
            <div class="link-title">TOP排名</div>
            <div class="link-desc">夏普比率排行榜</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- TOP基金 -->
    <div class="top-funds">
      <h2>🏆 TOP 10 基金</h2>
      <el-card v-loading="loading">
        <el-table :data="topFunds" stripe>
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="fundCode" label="代码" width="100" />
          <el-table-column prop="fundName" label="名称" />
          <el-table-column prop="sharpeRatio1y" label="夏普比率" width="100">
            <template #default="{ row }">
              <el-tag :type="row.sharpeRatio1y > 2 ? 'success' : 'info'">
                {{ row.sharpeRatio1y?.toFixed(2) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="return1y" label="近1年收益" width="100">
            <template #default="{ row }">
              <span :class="row.return1y > 0 ? 'positive' : 'negative'">
                {{ row.return1y?.toFixed(2) }}%
              </span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" @click="viewDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
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

const viewDetail = (row) => {
  router.push(`/fund/${row.fundCode}`)
}
</script>

<style scoped>
.welcome-section {
  margin-bottom: 30px;
}

.welcome-header {
  text-align: center;
}

.welcome-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 10px;
}

.welcome-header p {
  color: #909399;
  font-size: 16px;
}

.quick-stats {
  margin-top: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.stat-icon {
  font-size: 36px;
  margin-right: 15px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.quick-links {
  margin-bottom: 30px;
}

.quick-links h2 {
  margin-bottom: 20px;
  color: #303133;
}

.link-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s;
}

.link-card:hover {
  transform: translateY(-5px);
}

.link-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.link-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.link-desc {
  font-size: 14px;
  color: #909399;
}

.top-funds h2 {
  margin-bottom: 20px;
  color: #303133;
}

.positive {
  color: #67c23a;
}

.negative {
  color: #f56c6c;
}
</style>
