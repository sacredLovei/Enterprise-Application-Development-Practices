import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/request'

const routes = [
  { path: '/', redirect: '/overview' },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录', public: true } },
  { path: '/overview', name: 'overview', component: () => import('../views/Overview.vue'), meta: { title: '设备地图总览' } },
  { path: '/devices', name: 'devices', component: () => import('../views/Devices.vue'), meta: { title: '设备台账' } },
  // S99：任务管理拆二级子项——下发任务（大地图）/ 任务列表（满幅表格）
  {
    path: '/tasks',
    component: () => import('../views/Tasks.vue'),
    redirect: '/tasks/dispatch',
    meta: { title: '任务管理' },
    children: [
      { path: 'dispatch', name: 'tasks-dispatch', component: () => import('../views/TasksDispatch.vue'), meta: { title: '任务管理 · 下发任务' } },
      { path: 'list', name: 'tasks-list', component: () => import('../views/TasksList.vue'), meta: { title: '任务管理 · 任务列表' } }
    ]
  },
  // S99：告警中心拆母/子项——告警列表（母）/ 告警详情（子路由，照片满幅）
  {
    path: '/alarms',
    component: () => import('../views/Alarms.vue'),
    redirect: '/alarms/list',
    meta: { title: '告警中心' },
    children: [
      { path: 'list', name: 'alarms-list', component: () => import('../views/AlarmsList.vue'), meta: { title: '告警中心' } },
      { path: 'detail/:alarmId', name: 'alarms-detail', component: () => import('../views/AlarmDetail.vue'), meta: { title: '告警中心 · 详情' } }
    ]
  },
  { path: '/stats', name: 'stats', component: () => import('../views/Stats.vue'), meta: { title: '统计看板' } },
  { path: '/:pathMatch(.*)*', redirect: '/overview' }
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
