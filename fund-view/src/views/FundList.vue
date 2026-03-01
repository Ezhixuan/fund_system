<template>
  <div class="fund-list-page">
    <!-- 搜索栏 -->
    <div class="search-box">
      <div class="search-input-wrapper">
        <input
          v-model="searchForm.keyword"
          type="text"
          class="search-input"
          placeholder="搜索基金代码、名称或拼音..."
          @keyup.enter="handleSearch"
          @input="onKeywordInput"
        />
        <button class="search-btn" @click="handleSearch">
          <el-icon :size="24"><Search /></el-icon>
        </button>
      </div>
      
      <!-- 筛选标签 -->
      <div class="filter-tags">
        <button 
          v-for="type in fundTypes" 
          :key="type.value"
          class="filter-tag"
          :class="{ active: searchForm.fundType === type.value }"
          @click="toggleFilter('fundType', type.value)"
        >
          {{ type.label }}
        </button>
      </div>
      
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0 && showSuggestions" class="suggestions-dropdown">
        <div
          v-for="item in suggestions"
          :key="item.fundCode"
          class="suggestion-item"
          @click="goToDetail(item)"
        >
          <span class="suggestion-code">{{ item.fundCode }}</span>
          <span class="suggestion-name">{{ item.fundName }}</span>          <span class="suggestion-type">{{ item.fundType }}</span>
        </div>
      </div>
    </div>
    
    <!-- 结果列表 -->
    <div class="results-section" v-loading="loading">
      <div class="results-header">
        <span class="results-count">共 {{ total }} 只基金</span>
      </div>
      
      <div class="fund-cards">
        <div
          v-for="fund in fundList"
          :key="fund.fundCode"
          class="fund-card"
          @click="goToDetail(fund)"
        >
          <div class="fund-card-header">
            <div class="fund-card-info">
              <div class="fund-card-name">{{ fund.fundName }}</div>
              <div class="fund-card-code">{{ fund.fundCode }}</div>
            </div>
            
            <div class="fund-card-tags">
              <span v-if="fund.qualityLevel" class="tag tag-primary">
                {{ fund.qualityLevel }}级
              </span>
              <span class="tag">{{ fund.fundType || '未知' }}</span>
            </div>
          </div>
          
          <div class="fund-card-body">
            <div class="info-row">
              <span class="info-label">基金经理</span>
              <span class="info-value">{{ fund.managerName || '-' }}</span>
            </div>            
            <div class="info-row">
              <span class="info-label">基金公司</span>
              <span class="info-value">{{ fund.companyName || '-' }}</span>
            </div>
          </div>
          
          <div class="fund-card-footer">
            <span class="view-link">查看详情 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-if="!loading && fundList.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon :size="64"><Search /></el-icon>
        </div>
        <div class="empty-text">未找到相关基金</div>
        <div class="empty-subtext">尝试其他关键词或筛选条件</div>
      </div>
      
      <!-- 分页 -->
      <div v-if="total > searchForm.size" class="pagination-wrapper">
        <button 
          class="page-btn" 
          :disabled="searchForm.page === 1"
          @click="changePage(searchForm.page - 1)"
        >
          <el-icon><ArrowLeft /></el-icon> 上一页
        </button>
        
        <span class="page-info">第 {{ searchForm.page }} 页</span>
        
        <button 
          class="page-btn" 
          :disabled="searchForm.page * searchForm.size >= total"
          @click="changePage(searchForm.page + 1)"
        >
          下一页 <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </div>
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
const showSuggestions = ref(false)

const searchForm = reactive({
  keyword: '',
  fundType: '',
  page: 1,
  size: 12,
})

const fundTypes = [
  { label: '全部', value: '' },
  { label: '股票型', value: '股票型' },
  { label: '混合型', value: '混合型' },
  { label: '债券型', value: '债券型' },
  { label: '指数型', value: '指数型' },
  { label: '货币型', value: '货币型' },
  { label: 'QDII', value: 'QDII' },
]

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
      showSuggestions.value = true
    }
  } catch (error) {
    console.error(error)
  }
}, 300)

const onKeywordInput = () => {
  fetchSuggestions(searchForm.keyword)
  showSuggestions.value = true
}

// 搜索
const handleSearch = async (resetPage = true) => {
  showSuggestions.value = false
  loading.value = true
  if (resetPage) {
    searchForm.page = 1
  }
  try {
    const res = await fundApi.getFundList({ ...searchForm })
    if (res.success) {
      fundList.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// 切换筛选
const toggleFilter = (key, value) => {
  searchForm[key] = searchForm[key] === value ? '' : value
  handleSearch(true) // 筛选时重置页码
}

// 翻页
const changePage = (page) => {
  searchForm.page = page
  handleSearch(false) // 翻页时不重置页码
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 查看详情
const goToDetail = (fund) => {
  showSuggestions.value = false
  router.push(`/fund/${fund.fundCode}`)
}

// 点击外部关闭建议
const handleClickOutside = (e) => {
  if (!e.target.closest('.search-box')) {
    showSuggestions.value = false
  }
}

onMounted(() => {
  handleSearch()
  document.addEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.fund-list-page {
  max-width: 900px;
  margin: 0 auto;
}

/* 搜索框 */
.search-box {
  position: sticky;
  top: 80px;
  z-index: 50;
  margin-bottom: 24px;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
  padding: 16px 24px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 16px;
  background: var(--bg-primary);
  color: var(--text-primary);
  outline: none;
  transition: var(--transition);
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(0, 172, 238, 0.15);
}

.search-btn {
  width: 56px;
  height: 56px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--primary-color);
  color: white;
  font-size: 20px;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn:hover {
  background: var(--primary-hover);
  transform: scale(1.05);
}

/* 筛选标签 */
.filter-tags {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.filter-tag {
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.filter-tag:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.filter-tag.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

/* 搜索建议下拉 */
.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 68px;
  margin-top: 8px;
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-color);
  overflow: hidden;
  z-index: 100;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 1px solid var(--border-color);
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover {
  background: var(--bg-hover);
}

.suggestion-code {
  font-weight: 700;
  color: var(--primary-color);
  min-width: 60px;
}

.suggestion-name {
  flex: 1;
  font-weight: 600;
  color: var(--text-primary);
}

.suggestion-type {
  font-size: 13px;
  color: var(--text-secondary);
  padding: 4px 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

/* 结果区域 */
.results-section {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  padding: 24px;
}

.results-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.results-count {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);
}

/* 基金卡片网格 */
.fund-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.fund-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 20px;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid transparent;
}

.fund-card:hover {
  background: var(--bg-primary);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.fund-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.fund-card-info {
  min-width: 0;
}

.fund-card-name {
  font-weight: 700;
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fund-card-code {
  font-size: 13px;
  color: var(--text-secondary);
}

.fund-card-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.fund-card-body {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.info-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.fund-card-footer {
  text-align: right;
}

.view-link {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-subtext {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.page-btn {
  padding: 10px 20px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-primary);
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 768px) {
  .fund-cards {
    grid-template-columns: 1fr;
  }
  
  .search-input-wrapper {
    flex-direction: column;
  }
  
  .search-btn {
    width: 100%;
    height: 48px;
  }
  
  .suggestions-dropdown {
    right: 0;
  }
}
</style>
