<template>
  <div class="app">
    <!-- 顶部导航 -->
    <header class="app-header">
      <div class="header-container">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">📈</span>
          <span class="logo-text">基金智选</span>
        </div>
        
        <nav class="nav-menu">
          <router-link 
            v-for="item in navItems" 
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: $route.path === item.path }"
          >
            {{ item.name }}
          </router-link>
        </nav>
      </div>
    </header>
    
    <!-- 主内容 -->
    <main class="app-main">
      <div class="main-container">
        <router-view />
      </div>
    </main>
    
    <!-- 底部 -->
    <footer class="app-footer">
      <p>基金智选 © 2026</p>
    </footer>
  </div>
</template>

<script setup>
const navItems = [
  { path: '/', name: '首页' },
  { path: '/funds', name: '基金搜索' },
  { path: '/portfolio', name: '持仓管理' },
]
</script>

<style>
/* Twitter/X 风格全局样式 */
:root {
  --primary-color: #00acee;
  --primary-hover: #0095d1;
  --bg-primary: #ffffff;
  --bg-secondary: #f7f9fa;
  --bg-hover: rgba(0, 172, 238, 0.1);
  --text-primary: #0f1419;
  --text-secondary: #536471;
  --border-color: #eff3f4;
  --radius-sm: 9999px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.08);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.1);
  --transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  line-height: 1.5;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 - Twitter 风格 */
.app-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.logo:hover {
  background: var(--bg-hover);
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #00acee 0%, #1d9bf0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 导航链接 */
.nav-menu {
  display: flex;
  gap: 8px;
}

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

/* 主内容 */
.app-main {
  flex: 1;
  padding: 24px 20px;
}

.main-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 底部 */
.app-footer {
  background: var(--bg-primary);
  border-top: 1px solid var(--border-color);
  padding: 24px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

/* Twitter 风格按钮 */
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
  position: relative;
  overflow: hidden;
}

.btn-primary:hover {
  background: var(--primary-hover);
  transform: scale(1.03);
  box-shadow: var(--shadow-md);
}

.btn-primary:active {
  transform: scale(0.98);
}

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

.btn-outline:active {
  transform: scale(0.98);
}

/* Twitter 风格卡片 */
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

/* 输入框样式 */
.input-twitter {
  width: 100%;
  padding: 16px 20px;
  border: 1px solid var(--border-color);
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
  box-shadow: 0 0 0 3px rgba(0, 172, 238, 0.15);
}

/* 标签样式 */
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

/* 响应式 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
    height: 56px;
  }
  
  .logo-text {
    display: none;
  }
  
  .nav-link {
    padding: 8px 12px;
    font-size: 14px;
  }
  
  .app-main {
    padding: 16px;
  }
}
</style>
