<script setup>
// S99：任务管理布局壳——二级子项（下发任务 / 任务列表），参考 TDesign Starter 母-子导航
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
