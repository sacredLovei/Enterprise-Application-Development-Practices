import { ref, watch, onUnmounted } from 'vue'
import request from '../api/request'

/**
 * S96：受保护接口的图片加载（带令牌取图）。
 *
 * 背景（实测缺陷）：浏览器的 `<img src="/api/files/...">` 请求**不会携带 Authorization 头**
 * （本项目的令牌存在 localStorage，不是 Cookie），而 `/api/files/**` 在 S88 之后需要鉴权 →
 * 图片一律 401，界面表现为"告警中心的图没了"。
 *
 * 方案：用 axios（自动带 Bearer 令牌）以 blob 拉取图片，再转成 objectURL 供 `<img>` 使用；
 * objectURL 在路径变化或组件卸载时回收，避免内存泄漏。
 * 不采用"把令牌放进 URL 查询参数"的方案——那会把令牌写进浏览器历史与日志。
 *
 * @param {import('vue').Ref<string>} pathRef 形如 '/files/ALM-xxx'（相对 baseURL /api）
 * @returns {import('vue').Ref<string>} objectURL（未加载完成时为空串）
 */
export function useAuthImage(pathRef) {
  const src = ref('')
  let currentUrl = ''

  function release() {
    if (currentUrl) {
      URL.revokeObjectURL(currentUrl)
      currentUrl = ''
    }
  }

  async function load(path) {
    release()
    src.value = ''
    if (!path) return
    try {
      const blob = await request.get(path, { responseType: 'blob' })
      // 响应拦截器返回 res.data —— blob 响应下即为 Blob 本身
      if (blob instanceof Blob) {
        currentUrl = URL.createObjectURL(blob)
        src.value = currentUrl
      } else {
        console.warn('[authImage] unexpected response type for', path, blob)
      }
    } catch (e) {
      // 404（无该图）与 401（未登录）都在此静默处理：界面显示占位提示而非裂图
      console.warn('[authImage] load failed', path, e?.response?.status)
    }
  }

  watch(pathRef, (p) => { load(p) }, { immediate: true })
  onUnmounted(release)

  return src
}
