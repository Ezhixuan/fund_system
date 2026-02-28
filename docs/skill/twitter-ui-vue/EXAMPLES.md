# Twitter UI Vue 使用示例

## 快速开始

### 1. 安装依赖
```bash
npm install vue@3 vue-router@4
```

### 2. 复制样式文件
将 `twitter-ui.css` 复制到项目 `src/styles/` 目录

### 3. 引入样式
```javascript
// main.js
import { createApp } from 'vue'
import App from './App.vue'
import './styles/twitter-ui.css'

createApp(App).mount('#app')
```

### 4. 使用组件
```vue
<template>
  <div class="container">
    <!-- 按钮 -->
    <button class="btn-primary">主要按钮</button>
    <button class="btn-outline">轮廓按钮</button>
    
    <!-- 卡片 -->
    <div class="card">
      <div class="card-header">卡片标题</div>
      <div class="card-body">卡片内容</div>
    </div>
    
    <!-- 输入框 -->
    <input class="input-twitter" placeholder="请输入..." />
    
    <!-- 标签 -->
    <span class="tag tag-primary">主要</span>
    <span class="tag tag-success">成功</span>
    <span class="tag tag-danger">危险</span>
  </div>
</template>
```

## 完整页面示例

### 登录页
```vue
<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>欢迎回来</h1>
        <p>请登录您的账户</p>
      </div>
      
      <div class="login-form">
        <input 
          v-model="form.username" 
          class="input-twitter" 
          placeholder="用户名"
        />
        
        <input 
          v-model="form.password" 
          type="password"
          class="input-twitter" 
          placeholder="密码"
        />
        
        <button class="btn-primary btn-lg" @click="login">
          登录
        </button>
      </div>
      
      <div class="login-footer">
        <span>还没有账户？</span>
        <a href="#" class="nav-link">立即注册</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const form = reactive({
  username: '',
  password: ''
})

const login = () => {
  console.log('登录:', form)
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  padding: 20px;
}

.login-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 48px;
  width: 100%;
  max-width: 400px;
  box-shadow: var(--shadow-md);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.login-header p {
  color: var(--text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-form .btn-primary {
  margin-top: 8px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  color: var(--text-secondary);
}
</style>
```

### 仪表盘页
```vue
<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="grid-4">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-label">{{ stat.label }}</div>
      </div>
    </div>
    
    <!-- 内容区 -->
    <div class="grid-2 mt-4">
      <div class="card">
        <div class="card-header">最近活动</div>
        <div class="card-body">
          <div v-for="item in activities" :key="item.id" class="activity-item">
            <span class="tag" :class="'tag-' + item.type">{{ item.tag }}</span>
            <span>{{ item.desc }}</span>
          </div>
        </div>
      </div>
      
      <div class="card">
        <div class="card-header">快捷操作</div>
        <div class="card-body">
          <button class="btn-outline w-full mb-4">新建项目</button>
          <button class="btn-outline w-full mb-4">导入数据</button>
          <button class="btn-primary w-full">查看报告</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const stats = [
  { icon: '📊', value: '1,234', label: '总访问量' },
  { icon: '👥', value: '567', label: '用户数' },
  { icon: '💰', value: '¥89,000', label: '销售额' },
  { icon: '📈', value: '+12.5%', label: '增长率' },
]

const activities = [
  { id: 1, tag: '成功', desc: '订单 #1234 已完成', type: 'success' },
  { id: 2, tag: '警告', desc: '库存不足提醒', type: 'warning' },
  { id: 3, tag: '信息', desc: '新用户注册', type: 'primary' },
]
</script>

<style scoped>
.stat-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.stat-card:hover {
  border-color: var(--primary-color);
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
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

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.activity-item:last-child {
  border-bottom: none;
}

.w-full {
  width: 100%;
}
</style>
```

## 自定义主题

### 修改主色调
```css
:root {
  --primary-color: #ff6b6b;  /* 改为珊瑚色 */
  --primary-hover: #ee5a5a;
}
```

### 调整圆角
```css
:root {
  --radius-sm: 8px;   /* 小圆角 */
  --radius-md: 12px;
  --radius-lg: 16px;
}
```

### 暗色主题
```css
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #15202b;
    --bg-secondary: #1e2732;
    --text-primary: #e7e9ea;
    --text-secondary: #8899a6;
    --border-color: #38444d;
  }
}
```

## 常见问题

### Q: 如何修改按钮大小？
使用工具类 `.btn-sm` 或 `.btn-lg`，或自定义 padding:
```css
.my-btn {
  padding: 8px 16px;
  font-size: 13px;
}
```

### Q: 卡片如何添加点击效果？
```vue
<div class="card clickable" @click="handleClick">...</div>
```

```css
.clickable {
  cursor: pointer;
}

.clickable:hover {
  border-color: var(--primary-color);
  transform: translateY(-2px);
}
```

### Q: 如何实现加载状态？
```vue
<button class="btn-primary" :disabled="loading">
  {{ loading ? '加载中...' : '提交' }}
</button>
```
