import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/overview' },
  { path: '/overview', name: 'overview', component: () => import('../views/Overview.vue'), meta: { title: '设备地图总览' } },
  { path: '/devices', name: 'devices', component: () => import('../views/Devices.vue'), meta: { title: '设备台账' } },
  { path: '/tasks', name: 'tasks', component: () => import('../views/Tasks.vue'), meta: { title: '任务管理' } },
  { path: '/alarms', name: 'alarms', component: () => import('../views/Alarms.vue'), meta: { title: '告警中心' } },
  { path: '/stats', name: 'stats', component: () => import('../views/Stats.vue'), meta: { title: '统计看板' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
