// S100：全局 Toast 通知——极简事件总线 + ToastHost 容器渲染（零依赖）
// 用法：import { toast } from '../utils/toast'; toast.success('已下发'); toast.error('失败原因')
function emit(type, message, duration) {
  window.dispatchEvent(new CustomEvent('app-toast', {
    detail: { type, message, duration, id: Date.now() + Math.random() }
  }))
}

export const toast = {
  success: (message, duration = 3200) => emit('success', message, duration),
  error: (message, duration = 4200) => emit('error', message, duration),
  info: (message, duration = 3200) => emit('info', message, duration)
}
