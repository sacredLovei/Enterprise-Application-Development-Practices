<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const backend = ref('')

// 展示最近一次请求命中的后端实例（演示负载均衡效果，设计报告 5.2.6）
setInterval(() => {
  backend.value = window.__lastBackendInstance || ''
}, 1000)
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand">🚁 空地协同巡检平台</div>
      <nav class="nav">
        <RouterLink to="/overview">地图总览</RouterLink>
        <RouterLink to="/devices">设备台账</RouterLink>
        <RouterLink to="/tasks">任务管理</RouterLink>
        <RouterLink to="/alarms">告警中心</RouterLink>
        <RouterLink to="/stats">统计看板</RouterLink>
      </nav>
      <div class="meta">
        <span v-if="backend" class="instance">后端实例：{{ backend }}</span>
      </div>
    </header>
    <main class="content">
      <h2 class="page-title">{{ route.meta.title }}</h2>
      <RouterView />
    </main>
  </div>
</template>
