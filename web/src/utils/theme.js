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
 * 从触发点 (x,y) 切换主题（方向性动效，用户定制）：
 * - 切亮色：新画面从点击点圆形向外扩散（reveal）；
 * - 切暗色：旧画面（亮色）向点击点圆形收缩消失（contract），露出下层暗色。
 * 不支持 View Transitions / prefers-reduced-motion 时直接切换。
 */
export function toggleThemeAt(x, y) {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const target = getTheme() === 'dark' ? 'light' : 'dark'
  if (document.startViewTransition && !reduced) {
    document.documentElement.style.setProperty('--vt-x', x + 'px')
    document.documentElement.style.setProperty('--vt-y', y + 'px')
    document.documentElement.dataset.vt = 'to-' + target
    const vt = document.startViewTransition(() => applyTheme(target))
    vt.finished.finally(() => { delete document.documentElement.dataset.vt })
    return getTheme()
  }
  return applyTheme(target)
}
