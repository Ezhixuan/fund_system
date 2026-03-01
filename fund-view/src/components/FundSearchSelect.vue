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
      options.value = res.data || []
    } catch (error) {
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
