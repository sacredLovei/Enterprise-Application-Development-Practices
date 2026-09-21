// S97-b：主题读写与切换（localStorage: inspection_theme——沿用 S95 回滚检测同键口径）
// S102：toggleTheme 圆心固定为屏幕中心的方向性过渡（View Transitions API，降级为直接切换）——
// 切亮色：新画面从屏幕中心圆形向外扩散；切暗色：旧画面（亮色）向屏幕中心圆形收缩消失。
const KEY = 'inspection_theme'

export function getTheme() {
  return localStorage.getItem(KEY) === 'dark' ? 'dark' : 'light'
}

export function applyTheme(theme) {
  const t = theme === 'dark' ? 'dark' : 'light'
  document.documentElement.dataset.theme = t
  localStorage.setItem(KEY, t)
  // 通知监听方（ECharts 重渲染 / 地图底图切换）
  window.dispatchEvent(new CustomEvent('inspection-theme-changed', { detail: { theme: t } }))
  return t
}

export function toggleTheme() {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const target = getTheme() === 'dark' ? 'light' : 'dark'
  if (document.startViewTransition && !reduced) {
    document.documentElement.style.setProperty('--vt-x', window.innerWidth / 2 + 'px')
    document.documentElement.style.setProperty('--vt-y', window.innerHeight / 2 + 'px')
    document.documentElement.dataset.vt = 'to-' + target
    const vt = document.startViewTransition(() => applyTheme(target))
    vt.finished.finally(() => { delete document.documentElement.dataset.vt })
    return getTheme()
  }
  return applyTheme(target)
}
