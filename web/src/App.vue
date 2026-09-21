<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { clearAuth, getUser } from './api/request'

const route = useRoute()
const router = useRouter()
const backend = ref('')
const user = ref(getUser())

// 登录态随路由变化刷新（App 实例常驻，登录成功跳转后需要重新读取 localStorage）
watch(() => route.fullPath, () => { user.value = getUser() })

const isLogin = computed(() => route.meta.public === true)
const roleText = computed(() => {
  const r = user.value?.role
  return ({ ADMIN: '管理员', OPERATOR: '值班员' })[r] || r || ''
})

// 展示最近一次请求命中的后端实例（演示负载均衡效果，设计报告 5.2.6）
setInterval(() => {
  backend.value = window.__lastBackendInstance || ''
}, 1000)

// S88：退出登录——通知后端吊销令牌（写入 MongoDB 吊销表），再清理本地登录态
async function logout() {
  try {
    await request.post('/auth/logout')
  } catch (e) {
    console.warn('[Auth] 登出接口调用失败（令牌可能已过期）', e?.response?.status)
  }
  clearAuth()
  user.value = null
  router.replace('/login')
}
</script>

<template>
  <!-- 登录页：独立全屏布局，无侧边导航 -->
  <RouterView v-if="isLogin" />

  <div v-else class="shell">
    <!-- 左侧导航：毛玻璃白 + 灰色分隔线 -->
    <aside class="sidebar">
      <div class="brand">🚁 空地协同巡检平台</div>
      <nav class="nav">
        <RouterLink to="/overview">🗺️ 地图总览</RouterLink>
        <RouterLink to="/devices">📟 设备台账</RouterLink>
        <RouterLink to="/tasks">📋 任务管理</RouterLink>
        <RouterLink to="/alarms">🚨 告警中心</RouterLink>
        <RouterLink to="/stats">📊 统计看板</RouterLink>
      </nav>
      <div class="side-footer">
        <span v-if="backend" class="instance">后端：{{ backend }}</span>
        <span v-if="user" class="user">👤 {{ user.displayName || user.username }} · {{ roleText }}</span>
        <button v-if="user" class="logout" type="button" @click="logout">退出登录</button>
        <span class="ver">v0.7 · 空地协同</span>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="content">
      <h2 class="page-title">{{ route.meta.title }}</h2>
      <RouterView />
    </main>
  </div>
</template>
