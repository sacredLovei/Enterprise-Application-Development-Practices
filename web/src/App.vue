<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { clearAuth, getUser } from './api/request'
import AppIcon from './components/AppIcon.vue'
import ToastHost from './components/ToastHost.vue'
import { getTheme, toggleTheme } from './utils/theme'

const route = useRoute()
const router = useRouter()
const backend = ref('')
const user = ref(getUser())
const theme = ref(getTheme())   // S97-b：亮/暗主题状态（按钮图标随动）

// S102 补充：页面切换过渡名——单层方案（替代"外层淡出+内层水平滑动"的嵌套方案，
// 嵌套 Transition × 异步路由组件存在切换失效问题）：
// 同模块子页间（任务/告警的子项）→ 方向感知水平滑动；跨模块 → 上移淡出。
const transName = ref('page')
const depthOf = (p) => {
  if (p.startsWith('/tasks/dispatch')) return { m: '/tasks', d: 0 }
  if (p.startsWith('/tasks/list')) return { m: '/tasks', d: 1 }
  if (p.startsWith('/alarms/detail')) return { m: '/alarms', d: 1 }
  if (p.startsWith('/alarms/list')) return { m: '/alarms', d: 0 }
  return { m: p, d: 0 }
}
watch(() => route.path, (to, from) => {
  const a = depthOf(to)
  const b = depthOf(from)
  transName.value = a.m === b.m && a.m.startsWith('/') && b.m !== '/'
    ? (a.d > b.d ? 'slide-left' : 'slide-right')
    : 'page'
})

// S98 修复（用户反馈"点暗色模式没生效"）：S97-b 提交的 App.vue 模板含主题按钮，
// 但 script 侧的 theme ref 与 toggleTheme import 缺失——模板引用未定义绑定，
// 生产构建回退 ctx 查找得 undefined，点击即抛错且被 Vue 生产 errorHandler 吞掉
// （无控制台几乎不可感知）。修复：补齐 script 定义；事件改具名函数引用（最保守形态）。
// S102 补充：圆心固定屏幕中心的方向性过渡（View Transitions API，降级直接切换）。
function onToggleTheme() {
  theme.value = toggleTheme()
}

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
    <!-- 左侧导航（S97-a：SVG 图标 + 令牌化配色） -->
    <aside class="sidebar">
      <div class="brand"><AppIcon name="target" :size="20" /><span>空地协同巡检平台</span></div>
      <nav class="nav">
        <RouterLink to="/overview"><AppIcon name="map" /><span>地图总览</span></RouterLink>
        <RouterLink to="/devices"><AppIcon name="cpu" /><span>设备台账</span></RouterLink>
        <RouterLink to="/tasks"><AppIcon name="clipboard" /><span>任务管理</span></RouterLink>
        <RouterLink to="/alarms"><AppIcon name="alarm" /><span>告警中心</span></RouterLink>
        <RouterLink to="/stats"><AppIcon name="stats" /><span>统计看板</span></RouterLink>
      </nav>
      <div class="side-footer">
        <span v-if="backend" class="instance">后端：{{ backend }}</span>
        <span v-if="user" class="user user-line"><AppIcon name="user" :size="13" /><span>{{ user.displayName || user.username }} · {{ roleText }}</span></span>
        <button v-if="user" class="logout" type="button" @click="logout">
          <AppIcon name="logout" :size="14" /><span>退出登录</span>
        </button>
        <button
          class="logout theme-toggle"
          type="button"
          :title="theme === 'dark' ? '切换为亮色主题' : '切换为暗色主题'"
          @click="onToggleTheme"
        >
          <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" :size="14" />
          <span>{{ theme === 'dark' ? '亮色模式' : '暗色模式' }}</span>
        </button>
        <span class="ver">v0.7 · 空地协同</span>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="content">
      <h2 class="page-title">{{ route.meta.title }}</h2>
      <!-- S102 补充：页面切换平滑动效（out-in：旧页淡出上移、新页淡入下落）。
           key 用顶级路由段而非完整路径——子页切换（任务/告警的子项）时不重建布局壳，
           由壳内层的水平滑动过渡接管；跨模块导航仍触发本层过渡 -->
      <!-- S102 补充：单层页面过渡（key=完整路径；过渡名由 transName 按路由深度动态决定） -->
      <RouterView v-slot="{ Component }">
        <Transition :name="transName" mode="out-in" :duration="170">
          <component :is="Component" :key="route.path" />
        </Transition>
      </RouterView>
    </main>
  </div>

  <!-- S100：全局通知（登录页也可见） -->
  <ToastHost />
</template>
