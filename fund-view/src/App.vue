<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="logo">
        <span class="logo-icon">📈</span>
        <span class="logo-text">基金智选</span>
      </div>
      <el-menu
        mode="horizontal"
        :router="true"
        :default-active="$route.path"
        class="nav-menu"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/funds">基金搜索</el-menu-item>
        <el-menu-item index="/portfolio">持仓管理</el-menu-item>
      </el-menu>
    </el-header>
    
    <el-main class="app-main">
      <router-view />
    </el-main>
    
    <el-footer class="app-footer">
      <p>基金智选系统 © 2026</p>
    </el-footer>
  </el-container>
</template>

<script setup>
// App.vue
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.nav-menu {
  flex: 1;
  justify-content: flex-end;
}

.app-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.app-footer {
  text-align: center;
  color: #909399;
  padding: 20px;
}
</style>
