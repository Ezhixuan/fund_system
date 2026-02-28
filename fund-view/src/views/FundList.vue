<template>
  <div class="fund-list">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <div class="search-form">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索基金代码/名称/拼音"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
        
        <div class="filters">
          <el-select v-model="searchForm.fundType" placeholder="基金类型" clearable @change="handleSearch">
            <el-option label="股票型" value="股票型" />
            <el-option label="混合型" value="混合型" />
            <el-option label="债券型" value="债券型" />
            <el-option label="指数型" value="指数型" />
            <el-option label="QDII" value="QDII" />
            <el-option label="货币型" value="货币型" />
          </el-select>
          
          <el-select v-model="searchForm.riskLevel" placeholder="风险等级" clearable @change="handleSearch">
            <el-option label="低风险" :value="1" />
            <el-option label="中低风险" :value="2" />
            <el-option label="中风险" :value="3" />
            <el-option label="中高风险" :value="4" />
            <el-option label="高风险" :value="5" />
          </el-select>
        </div>
      </div>
      
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0 && searchForm.keyword" class="suggestions">
        <el-card shadow="hover">
          <div
            v-for="item in suggestions"
            :key="item.fundCode"
            class="suggestion-item"
            @click="goToDetail(item)"
          >
            <span class="code">{{ item.fundCode }}</span>
            <span class="name">{{ item.fundName }}</span>
            <span class="type">{{ item.fundType }}</span>
          </div>
        </el-card>
      </div>
    </el-card>
    
    <!-- 结果列表 -->
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>基金列表 (共 {{ total }} 条)</span>
        </div>
      </template>
      
      <el-table :data="fundList" stripe @row-click="handleRowClick">
        <el-table-column prop="fundCode" label="基金代码" width="100" />
        <el-table-column prop="fundName" label="基金名称" min-width="200">
          <template #default="{ row }">
            <div class="fund-name">
              <span>{{ row.fundName }}</span>
              <el-tag v-if="row.qualityLevel === 'S'" type="danger" size="small">S</el-tag>
              <el-tag v-else-if="row.qualityLevel === 'A'" type="success" size="small">A</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="fundType" label="类型" width="120" />
        
        <el-table-column prop="managerName" label="基金经理" width="120" />
        
        <el-table-column prop="riskLevel" label="风险" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" :type="getRiskType(row.riskLevel)">
              R{{ row.riskLevel }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="goToDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fundApi } from '@/api'
import { debounce } from '@/utils'

const router = useRouter()
const loading = ref(false)
const fundList = ref([])
const total = ref(0)
const suggestions = ref([])

const searchForm = reactive({
  keyword: '',
  fundType: '',
  riskLevel: '',
  page: 1,
  size: 20,
})

// 搜索建议
const fetchSuggestions = debounce(async (keyword) => {
  if (!keyword || keyword.length < 2) {
    suggestions.value = []
    return
  }
  try {
    const res = await fundApi.searchSuggest(keyword, 5)
    if (res.success) {
      suggestions.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}, 300)

watch(() => searchForm.keyword, (val) => {
  fetchSuggestions(val)
})

// 搜索
const handleSearch = async () => {
  loading.value = true
  try {
    const res = await fundApi.getFundList({
      keyword: searchForm.keyword,
      fundType: searchForm.fundType,
      riskLevel: searchForm.riskLevel,
      page: searchForm.page,
      size: searchForm.size,
    })
    if (res.success) {
      fundList.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// 查看详情
const goToDetail = (row) => {
  router.push(`/fund/${row.fundCode}`)
}

const handleRowClick = (row) => {
  goToDetail(row)
}

// 风险等级样式
const getRiskType = (level) => {
  const types = ['', 'success', 'success', 'warning', 'danger', 'danger']
  return types[level] || 'info'
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.fund-list {
  max-width: 1200px;
  margin: 0 auto;
}

.search-card {
  margin-bottom: 20px;
  position: relative;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__inner) {
  height: 48px;
  font-size: 16px;
}

.filters {
  margin-top: 15px;
  display: flex;
  gap: 15px;
}

.suggestions {
  position: absolute;
  top: 100%;
  left: 20px;
  right: 20px;
  z-index: 100;
  margin-top: 5px;
}

.suggestion-item {
  padding: 12px 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 15px;
  border-bottom: 1px solid #ebeef5;
}

.suggestion-item:hover {
  background-color: #f5f7fa;
}

.suggestion-item .code {
  color: #409eff;
  font-weight: bold;
  min-width: 60px;
}

.suggestion-item .name {
  flex: 1;
}

.suggestion-item .type {
  color: #909399;
  font-size: 12px;
}

.card-header {
  font-weight: bold;
}

.fund-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
