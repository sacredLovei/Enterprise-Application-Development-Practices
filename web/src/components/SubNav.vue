<script setup>
// S99：二级子项标签条（TDesign Starter 式）——任务管理/告警中心的母-子导航。
// 用 route.path 精确匹配激活态（RouterLink 的 active 类在父子路径间会互相误命中）。
import { useRoute } from 'vue-router'

const route = useRoute()

const props = defineProps({
  tabs: { type: Array, required: true }   // [{ to, label, match?: (path)=>bool }]
})

function isActive(tab) {
  if (tab.match) return tab.match(route.path)
  return route.path === tab.to || route.path.startsWith(tab.to + '/')
}
</script>

<template>
  <nav class="subnav">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      :class="{ active: isActive(tab) }"
      :aria-current="isActive(tab) ? 'page' : undefined"
    >{{ tab.label }}</RouterLink>
  </nav>
</template>
