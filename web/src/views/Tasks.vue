<script setup>
// S99：任务管理布局壳——二级子项（下发任务 / 任务列表），参考 TDesign Starter 母-子导航
// S102：子页水平滑动过渡上移至 App.vue 顶层单层实现（嵌套 Transition × 异步路由组件切换失效，
// 见 PLAN S102 补充），壳只负责 SubNav 与子路由出口
import SubNav from '../components/SubNav.vue'

const tabs = [
  { to: '/tasks/dispatch', label: '下发任务' },
  { to: '/tasks/list', label: '任务列表' }
]
</script>

<template>
  <div>
    <SubNav :tabs="tabs" />
    <RouterView />
  </div>
</template>
