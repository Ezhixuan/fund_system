<template>
  <div class="portfolio">
    <!-- 组合概览 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="overview-label">总市值</div>
          <div class="overview-value">{{ formatMoney(analysis.totalValue) }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="overview-label">总成本</div>
          <div class="overview-value">{{ formatMoney(analysis.totalCost) }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="overview-label">总收益</div>
          <div class="overview-value" :class="analysis.totalReturn > 0 ? 'positive' : 'negative'">
            {{ formatMoney(analysis.totalReturn) }}
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="overview-card">
          <div class="overview-label">收益率</div>
          <div class="overview-value" :class="analysis.totalReturnRate > 0 ? 'positive' : 'negative'">
            {{ formatPercent(analysis.totalReturnRate) }}
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="showTradeDialog = true">➕ 记录交易</el-button>
    </div>
    
    <!-- 持仓列表 -->
    <el-card v-loading="loading">
      <template #header><span>当前持仓 ({{ holdings.length }}只基金)</span></template>
      
      <el-table :data="holdings" stripe>
        <el-table-column prop="fundCode" label="基金代码" width="100" />
        
        <el-table-column prop="fundName" label="基金名称">
          <template #default="{ row }">
            <router-link :to="`/fund/${row.fundCode}`" class="fund-link">
              {{ row.fundName }}
            </router-link>
          </template>
        </el-table-column>
        
        <el-table-column prop="totalShares" label="持有份额" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.totalShares) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="avgCost" label="平均成本" width="100">
          <template #default="{ row }">
            {{ formatNumber(row.avgCost, 4) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="currentNav" label="当前净值" width="100">
          <template #default="{ row }">
            {{ formatNumber(row.currentNav, 4) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="currentValue" label="市值" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.currentValue) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="totalReturn" label="收益" width="120">
          <template #default="{ row }">
            <span :class="row.totalReturn > 0 ? 'positive' : 'negative'">
              {{ formatMoney(row.totalReturn) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="returnRate" label="收益率" width="100">
          <template #default="{ row }">
            <span :class="row.returnRate > 0 ? 'positive' : 'negative'">
              {{ formatPercent(row.returnRate) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="qualityLevel" label="等级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.qualityLevel" :type="getQualityType(row.qualityLevel)">
              {{ row.qualityLevel }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && holdings.length === 0" description="暂无持仓" />
    </el-card>
    
    <!-- 质量分布 -->
    <el-card v-if="analysis.qualityDistribution" class="distribution-card">
      <template #header><span>持仓质量分布</span></template>
      <el-row :gutter="20">
        <el-col v-for="(count, level) in analysis.qualityDistribution" :key="level" :span="4">
          <div class="dist-item">
            <el-tag :type="getQualityType(level)" size="large">{{ level }}级</el-tag>
            <span class="dist-count">{{ count }}只</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 记录交易弹窗 -->
    <el-dialog v-model="showTradeDialog" title="记录交易" width="500px">
      <el-form :model="tradeForm" label-width="100px">
        <el-form-item label="基金代码">
          <el-input v-model="tradeForm.fundCode" placeholder="如: 000001" />
        </el-form-item>
        
        <el-form-item label="交易日期">
          <el-date-picker v-model="tradeForm.tradeDate" type="date" placeholder="选择日期" />
        </el-form-item>
        
        <el-form-item label="交易类型">
          <el-radio-group v-model="tradeForm.tradeType">
            <el-radio :label="1">买入</el-radio>
            <el-radio :label="2">卖出</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="交易份额">
          <el-input-number v-model="tradeForm.tradeShare" :min="0" :precision="2" />
        </el-form-item>
        
        <el-form-item label="交易净值">
          <el-input-number v-model="tradeForm.tradePrice" :min="0" :precision="4" />
        </el-form-item>
        
        <el-form-item label="手续费">
          <el-input-number v-model="tradeForm.tradeFee" :min="0" :precision="2" />
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input v-model="tradeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showTradeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitTrade">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { portfolioApi } from '@/api'
import { formatNumber, formatPercent } from '@/utils'

const loading = ref(false)
const holdings = ref([])
const analysis = ref({})
const showTradeDialog = ref(false)

const tradeForm = reactive({
  fundCode: '',
  tradeDate: new Date(),
  tradeType: 1,
  tradeShare: 0,
  tradePrice: 0,
  tradeFee: 0,
  remark: '',
})

// 获取持仓数据
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
      tradeDate: tradeForm.tradeDate.toISOString().split('T')[0],
    })
    
    if (res.success) {
      ElMessage.success('交易记录成功')
      showTradeDialog.value = false
      fetchData()
      // 重置表单
      Object.assign(tradeForm, {
        fundCode: '',
        tradeDate: new Date(),
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

// 工具函数
const formatMoney = (val) => {
  if (val === undefined || val === null) return '¥0.00'
  return '¥' + Number(val).toFixed(2)
}

const getQualityType = (level) => {
  const types = { S: 'danger', A: 'success', B: 'primary', C: 'warning', D: 'info' }
  return types[level] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.portfolio {
  max-width: 1200px;
  margin: 0 auto;
}

.overview-row {
  margin-bottom: 20px;
}

.overview-card {
  text-align: center;
}

.overview-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.overview-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.action-bar {
  margin-bottom: 20px;
}

.fund-link {
  color: #409eff;
  text-decoration: none;
}

.fund-link:hover {
  text-decoration: underline;
}

.positive {
  color: #67c23a;
}

.negative {
  color: #f56c6c;
}

.distribution-card {
  margin-top: 20px;
}

.dist-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 15px;
}

.dist-count {
  font-size: 16px;
  color: #606266;
}
</style>
