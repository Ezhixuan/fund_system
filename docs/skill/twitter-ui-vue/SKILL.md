# Twitter UI Vue Skill

Vue 3 + Twitter/X 风格 UI 组件库
蓝白配色、圆角流线形、悬停滑入动画效果

## 快速开始

### 安装依赖
```bash
npm install vue@3 vue-router@4 pinia element-plus echarts
```

### 引入全局样式
在 `main.js` 或 `App.vue` 中引入：
```javascript
import '@/styles/twitter-ui.css'
```

## 核心特性

- 🎨 **蓝白配色** - #00acee 主色调
- 🔵 **圆角设计** - 流线形大圆角 16px-9999px
- ✨ **悬停动画** - 滑入/缩放/阴影效果
- 📱 **响应式** - PC/平板/手机完美适配
- 🎯 **组件丰富** - 按钮/卡片/输入框/标签等

## CSS 变量

```css
:root {
  /* 主色调 */
  --primary-color: #00acee;
  --primary-hover: #0095d1;
  
  /* 背景 */
  --bg-primary: #ffffff;
  --bg-secondary: #f7f9fa;
  --bg-hover: rgba(0, 172, 238, 0.1);
  
  /* 文字 */
  --text-primary: #0f1419;
  --text-secondary: #536471;
  
  /* 边框 */
  --border-color: #eff3f4;
  
  /* 圆角 */
  --radius-sm: 9999px;   /* 按钮/标签 */
  --radius-md: 16px;     /* 卡片 */
  --radius-lg: 24px;     /* 大卡片/模态框 */
  
  /* 阴影 */
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.08);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.1);
  
  /* 动画 */
  --transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
```

## 组件样式

### 按钮 Button

#### 主按钮 (实心)
```vue
<button class="btn-primary">主要按钮</button>
```

```css
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
}

.btn-primary:hover {
  background: var(--primary-hover);
  transform: scale(1.03);
  box-shadow: var(--shadow-md);
}

.btn-primary:active {
  transform: scale(0.98);
}
```

#### 轮廓按钮 (边框)
```vue
<button class="btn-outline">轮廓按钮</button>
```

```css
.btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  background: transparent;
  color: var(--primary-color);
  border: 1.5px solid var(--primary-color);
  border-radius: var(--radius-sm);
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

/* 悬停时背景从左滑入 */
.btn-outline::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 0;
  height: 100%;
  background: var(--primary-color);
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: -1;
}

.btn-outline:hover {
  color: white;
}

.btn-outline:hover::before {
  width: 100%;
}
```

### 卡片 Card

```vue
<div class="card">
  <div class="card-header">标题</div>
  <div class="card-body">内容</div>
</div>
```

```css
.card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: var(--transition);
}

.card:hover {
  box-shadow: var(--shadow-md);
}
```

### 输入框 Input

```vue
<input type="text" class="input-twitter" placeholder="请输入..." />
```

```css
.input-twitter {
  width: 100%;
  padding: 16px 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 16px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  transition: var(--transition);
  outline: none;
}

.input-twitter:focus {
  border-color: var(--primary-color);
  background: var(--bg-primary);
  box-shadow: 0 0 0 4px rgba(0, 172, 238, 0.15);
}
```

### 标签 Tag

```vue
<span class="tag tag-primary">标签</span>
<span class="tag tag-success">成功</span>
<span class="tag tag-danger">危险</span>
```

```css
.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}

.tag-primary {
  background: var(--bg-hover);
  color: var(--primary-color);
}

.tag-success {
  background: rgba(0, 186, 124, 0.1);
  color: #00ba7c;
}

.tag-danger {
  background: rgba(244, 33, 46, 0.1);
  color: #f4212e;
}
```

### 导航链接 NavLink

```vue
<router-link to="/" class="nav-link active">首页</router-link>
```

```css
.nav-link {
  padding: 10px 20px;
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 15px;
  border-radius: var(--radius-sm);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

/* 左侧滑入指示条 */
.nav-link::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 0;
  height: 100%;
  background: var(--primary-color);
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: -1;
}

.nav-link:hover {
  color: var(--primary-color);
}

.nav-link:hover::before {
  width: 4px;
}

.nav-link.active {
  color: var(--text-primary);
  background: var(--bg-hover);
}

/* 底部激活指示器 */
.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 4px;
  background: var(--primary-color);
  border-radius: 2px;
}
```

## 完整页面模板

### 首页模板
```vue
<template>
  <div class="home">
    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <h1>欢迎使用</h1>
      <p>智能分析，科学决策</p>
      <div class="actions">
        <button class="btn-primary">开始探索</button>
        <button class="btn-outline">了解更多</button>
      </div>
    </div>
    
    <!-- 统计网格 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-label">{{ stat.label }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome-card {
  background: linear-gradient(135deg, #00acee 0%, #1d9bf0 100%);
  border-radius: var(--radius-lg);
  padding: 48px 40px;
  color: white;
  text-align: center;
  margin-bottom: 32px;
}

.welcome-card h1 {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  padding: 24px;
  text-align: center;
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-color);
}

.stat-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
```

### 列表页模板
```vue
<template>
  <div class="list-page">
    <!-- 搜索栏 -->
    <div class="search-box">
      <input type="text" class="input-twitter" placeholder="搜索..." />
      <button class="btn-primary">搜索</button>
    </div>
    
    <!-- 卡片列表 -->
    <div class="card-grid">
      <div class="card" v-for="item in list" :key="item.id">
        <div class="card-header">{{ item.title }}</div>
        <div class="card-body">{{ item.desc }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

## 颜色参考

| 颜色 | 色值 | 用途 |
|------|------|------|
| Twitter蓝 | `#00acee` | 主色调、按钮、链接 |
| 成功绿 | `#00ba7c` | 正向指标、买入信号 |
| 危险红 | `#f4212e` | 负向指标、卖出信号 |
| 警告橙 | `#ffb347` | 警告提示 |
| 背景灰 | `#f7f9fa` | 页面背景 |
| 边框灰 | `#eff3f4` | 边框、分割线 |
| 主文字 | `#0f1419` | 标题、正文 |
| 次要文字 | `#536471` | 辅助说明 |

## 动画时间

| 动画 | 时长 | 用途 |
|------|------|------|
| 快速反馈 | 0.2s | 按钮点击 |
| 标准过渡 | 0.5s | 悬停效果 |
| 页面切换 | 0.3s | 路由动画 |

## 响应式断点

```css
/* 手机 */
@media (max-width: 768px) {
  /* 单列布局 */
}

/* 平板 */
@media (min-width: 769px) and (max-width: 1024px) {
  /* 双列布局 */
}

/* 桌面 */
@media (min-width: 1025px) {
  /* 多列布局 */
}
```

## 文件结构

```
src/
├── styles/
│   ├── twitter-ui.css      # 全局样式变量
│   ├── components.css      # 组件样式
│   └── animations.css      # 动画定义
├── components/
│   ├── TwitterButton.vue
│   ├── TwitterCard.vue
│   ├── TwitterInput.vue
│   └── TwitterTag.vue
└── views/
    ├── Home.vue
    ├── List.vue
    └── Detail.vue
```

## 最佳实践

1. **优先使用 CSS 变量** - 便于主题切换
2. **悬停必有过渡** - 保持交互流畅
3. **圆角保持一致** - 按钮9999px，卡片16px
4. **阴影层级分明** - sm/md 两个级别
5. **响应式优先** - 移动端体验一致

## 示例项目

参考实现：`fund-view/` 目录
- 首页渐变卡片
- 基金搜索筛选
- 详情页信号展示
- 持仓管理分布图

## 更新记录

- 2026-03-01: 初始版本，包含完整组件和页面模板
