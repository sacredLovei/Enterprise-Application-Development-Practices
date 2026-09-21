<script setup>
// S99：任务管理布局壳——二级子项（下发任务 / 任务列表），参考 TDesign Starter 母-子导航
// S102 补充：子页切换水平滑动（方向感知——向右子项切入则内容左移，反之右移）
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import SubNav from '../components/SubNav.vue'

const route = useRoute()

const tabs = [
  { to: '/tasks/dispatch', label: '下发任务' },
  { to: '/tasks/list', label: '任务列表' }
]

const ORDER = ['/tasks/dispatch', '/tasks/list']
const depth = computed(() => {
  const i = ORDER.indexOf(route.path)
  return i >= 0 ? i : 0
})

// 方向感知：切到更靠右的子项 → 新页从右侧滑入（slide-left）；反之从左滑入
const transName = ref('slide-left')
watch(depth, (nv, ov) => {
  transName.value = nv > ov ? 'slide-left' : 'slide-right'
})
</script>

<template>
  <div>
    <SubNav :tabs="tabs" />
    <RouterView v-slot="{ Component }">
      <Transition :name="transName" mode="out-in">
        <component :is="Component" :key="route.path" />
      </Transition>
    </RouterView>
  </div>
</template>

<style scoped>
/* 滑动过渡样式已上移至全局 style.css（异步路由组件不继承父 scoped 属性，S102 实测） */
</style>
