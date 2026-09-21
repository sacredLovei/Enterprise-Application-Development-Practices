import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/request'

const routes = [
  { path: '/', redirect: '/overview' },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录', public: true } },
  { path: '/overview', name: 'overview', component: () => import('../views/Overview.vue'), meta: { title: '设备地图总览' } },
  { path: '/devices', name: 'devices', component: () => import('../views/Devices.vue'), meta: { title: '设备台账' } },
  { path: '/tasks', name: 'tasks', component: () => import('../views/Tasks.vue'), meta: { title: '任务管理' } },
  { path: '/alarms', name: 'alarms', component: () => import('../views/Alarms.vue'), meta: { title: '告警中心' } },
  { path: '/stats', name: 'stats', component: () => import('../views/Stats.vue'), meta: { title: '统计看板' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// S88：前端路由守卫——未登录一律回登录页，并带回跳地址；已登录访问登录页直接进首页
router.beforeEach((to) => {
  const authed = !!getToken()
  if (to.meta.public) {
    return authed ? '/overview' : true
  }
  if (!authed) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
