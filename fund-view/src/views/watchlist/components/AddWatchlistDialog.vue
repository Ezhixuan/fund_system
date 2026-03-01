<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑关注' : '添加关注'"
    width="500px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      class="watchlist-form"
    >
      <!-- 基金选择 -->
      <el-form-item label="基金" prop="fundCode" v-if="!isEdit">
        <FundSearchSelect
          v-model="form.fundCode"
          v-model:name="form.fundName"
          placeholder="搜索基金名称或代码"
        />
      </el-form-item>

      <!-- 编辑时显示基金信息 -->
      <el-form-item label="基金" v-else>
        <div class="fund-display">
          <span class="fund-name">{{ form.fundName }}</span>
          <span class="fund-code">{{ form.fundCode }}</span>
        </div>
      </el-form-item>

      <!-- 关注类型 -->
      <el-form-item label="关注类型" prop="watchType">
        <el-radio-group v-model="form.watchType">
          <el-radio :label="1">
            <Icon icon="wallet" /> 持有中
          </el-radio>
          <el-radio :label="2">
            <Icon icon="star" /> 仅关注
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 目标收益率 -->
      <el-form-item label="目标收益率">
        <el-input-number
          v-model="form.targetReturn"
          :min="0"
          :max="100"
          :precision="2"
          placeholder="可选"
          class="percentage-input"
        >
          <template #suffix>%</template>
        </el-input-number>
        <div class="form-hint">达到目标收益率时提醒</div>
      </el-form-item>

      <!-- 止损线 -->
      <el-form-item label="止损线">
        <el-input-number
          v-model="form.stopLoss"
          :min="-50"
          :max="0"
          :precision="2"
          placeholder="可选"
          class="percentage-input"
        >
          <template #suffix>%</template>
        </el-input-number>
        <div class="form-hint">跌破止损线时提醒（负数）</div>
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注">
        <el-input
          v-model="form.notes"
          type="textarea"
          :rows="3"
          placeholder="添加备注信息（可选）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ isEdit ? '保存' : '添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Icon } from '@/components/Icon'
import FundSearchSelect from '@/components/FundSearchSelect.vue'
import { addWatchlist, updateWatchlist } from '@/api/watchlist'

const props = defineProps({
  modelValue: Boolean,
  editData: Object
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref(null)
const submitting = ref(false)

// 是否编辑模式
const isEdit = computed(() => !!props.editData)
const visible = computed(() => props.modelValue)

// 表单数据
const form = ref({
  fundCode: '',
  fundName: '',
  watchType: 2,
  targetReturn: null,
  stopLoss: null,
  notes: ''
})

// 表单校验规则
const rules = {
  fundCode: [
    { required: true, message: '请选择基金', trigger: 'change' }
  ],
  watchType: [
    { required: true, message: '请选择关注类型', trigger: 'change' }
  ]
}

// 监听编辑数据变化
watch(() => props.editData, (val) => {
  if (val) {
    form.value = {
      fundCode: val.fundCode,
      fundName: val.fundName,
      watchType: val.watchType,
      targetReturn: val.targetReturn,
      stopLoss: val.stopLoss,
      notes: val.notes
    }
  } else {
    resetForm()
  }
}, { immediate: true })

// 重置表单
const resetForm = () => {
  form.value = {
    fundCode: '',
    fundName: '',
    watchType: 2,
    targetReturn: null,
    stopLoss: null,
    notes: ''
  }
  formRef.value?.resetFields()
}

// 关闭弹窗
const handleClose = () => {
  emit('update:modelValue', false)
  resetForm()
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateWatchlist(props.editData.fundCode, form.value)
      ElMessage.success('保存成功')
    } else {
      await addWatchlist(form.value)
      ElMessage.success('添加成功')
    }
    emit('success')
    handleClose()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.watchlist-form {
  .fund-display {
    display: flex;
    align-items: center;
    gap: 8px;

    .fund-name {
      font-weight: 500;
    }

    .fund-code {
      color: #909399;
      font-size: 12px;
    }
  }

  .percentage-input {
    width: 150px;
  }

  .form-hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
