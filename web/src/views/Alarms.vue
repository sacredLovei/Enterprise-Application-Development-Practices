<script setup>
// S99-b：告警中心布局壳——母项（告警列表）/ 子项（告警详情独立子路由，照片满幅）
// 详情 tab 仅在详情路由时激活；列表态点击它不跳转（避免无 alarmId 的空详情）
// S102 补充：子页切换水平滑动（列表→详情从右滑入；详情→列表从左滑回）
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import SubNav from '../components/SubNav.vue'

const route = useRoute()

const depth = computed(() => (route.path.startsWith('/alarms/detail') ? 1 : 0))

const transName = ref('slide-left')
watch(depth, (nv, ov) => {
  transName.value = nv > ov ? 'slide-left' : 'slide-right'
})

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
