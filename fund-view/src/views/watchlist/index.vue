<template>
  <div class="watchlist-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>我的关注</h1>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog = true">
          <Icon icon="plus" /> 添加关注
        </el-button>
        <el-button @click="handleImportFromPortfolio">
          从持仓导入
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">关注基金</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value text-success">{{ stats.holding }}</div>
        <div class="stat-label">持有中</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value text-warning">{{ stats.watching }}</div>
        <div class="stat-label">仅关注</div>
      </el-card>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-radio-group v-model="filterType" @change="handleFilterChange">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="1">持有中</el-radio-button>
        <el-radio-button :label="2">仅关注</el-radio-button>
      </el-radio-group>
      
      <el-input
        v-model="searchKeyword"
        placeholder="搜索基金名称/代码"
        class="search-input"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <Icon icon="search" />
        </template>
      </el-input>
    </div>

    <!-- 关注列表 -->
    <el-table
      :data="filteredList"
      class="watchlist-table"
      v-loading="loading"
      @row-click="handleRowClick"
    >
      <el-table-column type="index" width="50" />
      
      <!-- 基金信息 -->
      <el-table-column label="基金" min-width="200">
        <template #default="{ row }">
          <div class="fund-info">
            <div class="fund-name">{{ row.fundName }}</div>
            <div class="fund-code">{{ row.fundCode }}</div>
          </div>
        </template>
      </el-table-column>

      <!-- 关注类型 -->
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.watchType === 1 ? 'success' : 'info'">
            {{ row.watchType === 1 ? '持有' : '关注' }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 目标/止损 -->
      <el-table-column label="目标/止损" width="150">
        <template #default="{ row }">
          <div v-if="row.targetReturn || row.stopLoss">
            <div v-if="row.targetReturn" class="target-return">
              目标: +{{ row.targetReturn }}%
            </div>
            <div v-if="row.stopLoss" class="stop-loss">
              止损: {{ row.stopLoss }}%
            </div>
          </div>
          <span v-else class="text-muted">未设置</span>
        </template>
      </el-table-column>

      <!-- 添加日期 -->
      <el-table-column label="添加日期" width="120">
        <template #default="{ row }">
          {{ formatDate(row.addDate) }}
        </template>
      </el-table-column>

      <!-- 备注 -->
      <el-table-column label="备注" min-width="150">
        <template #default="{ row }">
          <span class="text-muted">{{ row.notes || '-' }}</span>
        </template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="handleEdit(row)">
            编辑
          </el-button>
          <el-button type="danger" link @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty
      v-if="!loading && filteredList.length === 0"
      description="暂无关注的基金"
    >
      <el-button type="primary" @click="showAddDialog = true">
        添加关注
      </el-button>
    </el-empty>

    <!-- 添加/编辑弹窗 -->
    <AddWatchlistDialog
      v-model="showAddDialog"
      :edit-data="editingItem"
      @success="handleSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@/components/Icon'
import AddWatchlistDialog from './components/AddWatchlistDialog.vue'
import { getWatchlist, deleteWatchlist, importFromPortfolio } from '@/api/watchlist'

const loading = ref(false)
const watchlist = ref([])
const filterType = ref(null)
const searchKeyword = ref('')
const showAddDialog = ref(false)
const editingItem = ref(null)

// 统计数据
const stats = computed(() => {
  const total = watchlist.value.length
  const holding = watchlist.value.filter(item => item.watchType === 1).length
  const watching = watchlist.value.filter(item => item.watchType === 2).length
  return { total, holding, watching }
})

// 过滤后的列表
const filteredList = computed(() => {
  let list = watchlist.value
  
  // 按类型过滤
  if (filterType.value !== null) {
    list = list.filter(item => item.watchType === filterType.value)
  }
  
  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(item => 
      item.fundName.toLowerCase().includes(keyword) ||
      item.fundCode.toLowerCase().includes(keyword)
    )
  }
  
  return list
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getWatchlist()
    watchlist.value = res.data || []
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 筛选变化
const handleFilterChange = () => {
  // 筛选已在计算属性中处理
}

// 搜索
const handleSearch = () => {
  // 搜索已在计算属性中处理
}

// 行点击
const handleRowClick = (row) => {
  // 跳转到基金详情页
  window.open(`/fund/${row.fundCode}`, '_blank')
}

// 编辑
const handleEdit = (row) => {
  editingItem.value = { ...row }
  showAddDialog.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除对 "${row.fundName}" 的关注吗？`,
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteWatchlist(row.fundCode)
    ElMessage.success('移除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败')
    }
  }
}

// 从持仓导入
const handleImportFromPortfolio = async () => {
  try {
    const res = await importFromPortfolio()
    ElMessage.success(`成功导入 ${res.data.importedCount} 只基金`)
    loadData()
  } catch (error) {
    ElMessage.error('导入失败')
  }
}

// 添加/编辑成功
const handleSuccess = () => {
  loadData()
  editingItem.value = null
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.watchlist-page {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h1 {
      margin: 0;
      font-size: 24px;
    }
  }

  .stats-cards {
    display: flex;
    gap: 16px;
    margin-bottom: 20px;

    .stat-card {
      flex: 1;
      text-align: center;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;

        &.text-success {
          color: #67c23a;
        }

        &.text-warning {
          color: #e6a23c;
        }
      }

      .stat-label {
        margin-top: 8px;
        color: #909399;
        font-size: 14px;
      }
    }
  }

  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .search-input {
      width: 250px;
    }
  }

  .watchlist-table {
    .fund-info {
      .fund-name {
        font-weight: 500;
        color: #303133;
      }

      .fund-code {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }

    .target-return {
      color: #f56c6c;
    }

    .stop-loss {
      color: #67c23a;
    }

    .text-muted {
      color: #909399;
    }
  }
}
</style>
