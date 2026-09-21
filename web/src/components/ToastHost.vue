<script setup>
// S100：全局 Toast 容器——挂在 App.vue 根部，监听 utils/toast 事件并渲染右上角通知栈
import { ref, onMounted, onUnmounted } from 'vue'

const items = ref([])   // { id, type, message }
let seq = 0
const timers = new Map()

function onAppToast(e) {
  const { type, message, duration } = e.detail
  const id = ++seq
  items.value.push({ id, type, message })
  timers.set(id, setTimeout(() => dismiss(id), duration || 3200))
}

function dismiss(id) {
  const t = timers.get(id)
  if (t) { clearTimeout(t); timers.delete(id) }
  items.value = items.value.filter(i => i.id !== id)
}

onMounted(() => window.addEventListener('app-toast', onAppToast))
onUnmounted(() => {
  window.removeEventListener('app-toast', onAppToast)
  timers.forEach(clearTimeout)
})
</script>

<template>
  <div class="toast-host" aria-live="polite">
    <div v-for="i in items" :key="i.id" class="toast" :class="'toast-' + i.type" @click="dismiss(i.id)">
      <span class="toast-dot"></span>
      <span>{{ i.message }}</span>
    </div>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 380px;
}
.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  color: var(--text-1);
  border: 1px solid var(--border);
  border-left: 3px solid var(--info);
  border-radius: var(--radius-1);
  padding: 11px 14px;
  font-size: 13px;
  box-shadow: var(--shadow-2);
  cursor: pointer;
  animation: toast-in .22s ease-out;
}
.toast-success { border-left-color: var(--ok); }
.toast-error { border-left-color: var(--danger); }
.toast-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--info); flex-shrink: 0; }
.toast-success .toast-dot { background: var(--ok); }
.toast-error .toast-dot { background: var(--danger); }
@keyframes toast-in {
  from { opacity: 0; transform: translateX(16px); }
  to   { opacity: 1; transform: none; }
}
</style>
