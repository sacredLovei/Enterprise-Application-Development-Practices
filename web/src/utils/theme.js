// S97-b：主题读写与切换（localStorage: inspection_theme——沿用 S95 回滚检测同键口径）
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
  return applyTheme(getTheme() === 'dark' ? 'light' : 'dark')
}
