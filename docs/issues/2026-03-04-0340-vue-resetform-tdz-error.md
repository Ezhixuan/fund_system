# Issue: AddWatchlistDialog.vue 中 resetForm 函数暂时性死区错误

**Created**: 2026-03-04 03:40
**Status**: Fixed
**Priority**: High
**Fixed At**: 2026-03-04 03:41

## Problem Description

前端页面报错，AddWatchlistDialog 组件无法正常渲染：

```
AddWatchlistDialog.vue:151 Uncaught (in promise) ReferenceError:
Cannot access 'resetForm' before initialization
    at watch.immediate (AddWatchlistDialog.vue:151:5)
    at setup (AddWatchlistDialog.vue:140:1)
```

第二个错误（连锁反应）：
```
Uncaught (in promise) TypeError: Cannot read properties of null (reading 'parentNode')
```

## Environment

- Project: fund-system/fund-view
- File: `src/views/watchlist/components/AddWatchlistDialog.vue`
- Framework: Vue 3 + Composition API
- Node Version: 18+

## Analysis

### 问题代码位置

```javascript
// 第 139-153 行
watch(() => props.editData, (val) => {
  if (val) {
    form.value = { ... }
  } else {
    resetForm()  // <-- 第151行报错
  }
}, { immediate: true })

// 第 156-166 行
const resetForm = () => {
  form.value = { ... }
  formRef.value?.resetFields()
}
```

### 错误原因

这是 JavaScript 的 **暂时性死区（Temporal Dead Zone, TDZ）** 错误。

1. 使用了 `{ immediate: true }` 选项，watch 在组件 setup 时会**立即执行**回调
2. 但 `resetForm` 函数定义在 watch 语句**之后**
3. 当 watch 立即执行时，`resetForm` 还未初始化，导致 TDZ 错误

```
执行顺序：
1. watch 注册并立即执行 (immediate: true)
2. 调用 resetForm()
3. ❌ resetForm 还未定义 → ReferenceError
4. const resetForm = () => {}  // 这里才定义
```

## Root Cause

Vue 3 Composition API 中的函数和变量遵循 JavaScript 的声明提升规则。
使用 `immediate: true` 的 watch 会在声明时立即执行，而此时在其之后定义的函数还未初始化。

## Solution

### Option 1 (Recommended): 调整函数定义顺序

将 `resetForm` 函数定义移到 `watch` 之前：

```javascript
// 先定义函数
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

// 后使用 watch
watch(() => props.editData, (val) => {
  if (val) {
    form.value = { ... }
  } else {
    resetForm()  // 现在 resetForm 已定义
  }
}, { immediate: true })
```

### Option 2: 内联重置逻辑

在 watch 中直接内联重置逻辑，不调用函数：

```javascript
watch(() => props.editData, (val) => {
  if (val) {
    form.value = { ... }
  } else {
    // 直接内联重置逻辑
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
}, { immediate: true })
```

### Option 3: 移除 immediate 选项

如果不需要立即执行，可以移除 `immediate: true`：

```javascript
watch(() => props.editData, (val) => {
  if (val) {
    form.value = { ... }
  } else {
    resetForm()
  }
})  // 移除 immediate: true
```

**注意**：这可能会改变组件的行为，需要确认是否依赖立即执行。

## Action Items

- [ ] 修复 AddWatchlistDialog.vue 中的函数定义顺序
- [ ] 检查项目中其他组件是否有类似问题
- [ ] 运行前端测试验证修复

## Quick Fix

**立即修改文件**：`fund-view/src/views/watchlist/components/AddWatchlistDialog.vue`

将第 155-166 行的 `resetForm` 定义剪切到第 139 行（watch 之前）。

## Related

- Vue 3 文档: [Composition API - Setup](https://vuejs.org/api/composition-api-setup.html)
- MDN: [Temporal Dead Zone](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/let#temporal_dead_zone_tdz)
- 文件路径: `fund-view/src/views/watchlist/components/AddWatchlistDialog.vue`
