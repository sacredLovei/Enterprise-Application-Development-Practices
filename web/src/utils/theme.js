// S97-b：主题读写与切换（localStorage: inspection_theme——沿用 S95 回滚检测同键口径）
// S102 补充：toggleThemeAt 支持从触发点坐标做圆形扩散过渡（View Transitions API，降级为直接切换）
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

function flip() {
  return applyTheme(getTheme() === 'dark' ? 'light' : 'dark')
}

export function toggleTheme() {
  return flip()
}

/**
 * 从触发点 (x,y) 切换主题：优先 View Transitions 圆形扩散动效，
 * 不支持的浏览器 / prefers-reduced-motion 直接切换。
 */
export function toggleThemeAt(x, y) {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (document.startViewTransition && !reduced) {
    document.documentElement.style.setProperty('--vt-x', x + 'px')
    document.documentElement.style.setProperty('--vt-y', y + 'px')
    document.startViewTransition(flip)
    return getTheme()
  }
  return flip()
}
