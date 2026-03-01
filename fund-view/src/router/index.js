import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    path: '/funds',
    name: 'FundList',
    component: () => import('@/views/FundList.vue'),
  },
  {
    path: '/fund/:code',
    name: 'FundDetail',
    component: () => import('@/views/FundDetail.vue'),
  },
  {
    path: '/portfolio',
    name: 'Portfolio',
    component: () => import('@/views/Portfolio.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

// 关注列表路由
{
  path: '/watchlist',
  name: 'Watchlist',
  component: () => import('@/views/watchlist/index.vue'),
  meta: {
    title: '我的关注',
    icon: 'star'
  }
}
