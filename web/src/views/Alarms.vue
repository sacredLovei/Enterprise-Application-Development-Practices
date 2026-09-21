<script setup>
// S99-b：告警中心布局壳——母项（告警列表）/ 子项（告警详情独立子路由，照片满幅）
// 详情 tab 仅在详情路由时激活；列表态点击它不跳转（避免无 alarmId 的空详情）
// S102：子页水平滑动过渡上移至 App.vue 顶层单层实现，壳只负责 SubNav 与子路由出口
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SubNav from '../components/SubNav.vue'

const route = useRoute()

const tabs = computed(() => [
  { to: '/alarms/list', label: '告警列表' },
  {
    to: route.path.startsWith('/alarms/detail/') ? route.path : '/alarms/list',
    label: '告警详情',
    match: p => p.startsWith('/alarms/detail')
  }
])
</script>

<template>
  <div>
    <SubNav :tabs="tabs" />
    <RouterView />
  </div>
</template>
