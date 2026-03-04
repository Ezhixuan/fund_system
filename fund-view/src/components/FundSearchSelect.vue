<template>
  <div class="fund-search-select">
    <el-select
      :model-value="modelValue"
      filterable
      remote
      placeholder="搜索基金名称或代码"
      :remote-method="searchFunds"
      :loading="searching"
      @change="handleSelect"
      class="fund-select"
    >
      <el-option
        v-for="item in options"
        :key="item.fundCode"
        :label="`${item.fundName} (${item.fundCode})`"
        :value="item.fundCode"
      >
        <div class="fund-option">
          <span class="fund-name">{{ item.fundName }}</span>
          <span class="fund-code">{{ item.fundCode }}</span>
          <span class="fund-type">{{ item.fundType }}</span>
        </div>
      </el-option>
    </el-select>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { searchFund } from '@/api/fund'

const props = defineProps({
  modelValue: String,
  name: String
})

const emit = defineEmits(['update:modelValue', 'update:name'])

const options = ref([])
const searching = ref(false)
let searchTimeout = null

// 搜索基金
const searchFunds = async (query) => {
  if (!query || query.length < 2) {
    options.value = []
    return
  }

  // 防抖
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(async () => {
    searching.value = true
    try {
      const res = await searchFund(query)
      console.log('搜索基金响应:', res)

      // 处理不同可能的响应结构
      let list = []
      if (Array.isArray(res.data)) {
        list = res.data
      } else if (Array.isArray(res)) {
        list = res
      } else if (res.data && Array.isArray(res.data.data)) {
        list = res.data.data
      }

      // 兼容后端返回的下划线命名和驼峰命名
      options.value = list.map(item => ({
        fundCode: item.fundCode || item.fund_code || '',
        fundName: item.fundName || item.fund_name || '',
        fundType: item.fundType || item.fund_type || ''
      })).filter(item => item.fundCode && item.fundName)

      console.log('处理后的选项:', options.value)
    } catch (error) {
      console.error('搜索基金失败:', error)
      options.value = []
    } finally {
      searching.value = false
    }
  }, 300)
}

// 选择基金
const handleSelect = (val) => {
  emit('update:modelValue', val)
  const selected = options.value.find(item => item.fundCode === val)
  if (selected) {
    emit('update:name', selected.fundName)
  }
}
</script>

<style scoped lang="scss">
.fund-search-select {
  .fund-select {
    width: 100%;
  }

  .fund-option {
    display: flex;
    align-items: center;
    gap: 8px;

    .fund-name {
      flex: 1;
      font-weight: 500;
    }

    .fund-code {
      color: #909399;
      font-size: 12px;
    }

    .fund-type {
      font-size: 12px;
      color: #409eff;
      background: #ecf5ff;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}
</style>
