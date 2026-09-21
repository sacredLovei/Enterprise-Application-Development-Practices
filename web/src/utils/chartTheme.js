// S97-b：ECharts 双主题注册——inspection-light / inspection-dark
// 主题色取自 tokens.css 的 CSS 变量（经临时挂载元素读取任意主题的变量值），
// 保证图表色板与页面令牌永远同源，不重复维护。
import * as echarts from 'echarts'

const VAR_NAMES = [
  'chart-1', 'chart-2', 'chart-3', 'chart-4', 'chart-5', 'chart-6', 'chart-7',
  'text-1', 'text-2', 'text-3', 'bg-card', 'bg-inset', 'border', 'border-strong'
]

/** 读取指定主题下的令牌值：[data-theme] 选择器匹配任意元素，挂临时节点即可取到覆盖值 */
function readVars(theme) {
  const el = document.createElement('div')
  if (theme === 'dark') el.setAttribute('data-theme', 'dark')
  el.style.display = 'none'
  document.body.appendChild(el)
  const cs = getComputedStyle(el)
  const out = {}
  for (const n of VAR_NAMES) out[n.replace(/-/g, '_')] = cs.getPropertyValue('--' + n).trim()
  el.remove()
  return out
}

function buildTheme(theme) {
  const v = readVars(theme)
  return {
    color: [v.chart_1, v.chart_2, v.chart_3, v.chart_4, v.chart_5, v.chart_6, v.chart_7],
    backgroundColor: 'transparent',
    textStyle: { color: v.text_2, fontFamily: 'Inter, "HarmonyOS Sans SC", "Microsoft YaHei", sans-serif' },
    title: { textStyle: { color: v.text_1, fontSize: 14 } },
    legend: { textStyle: { color: v.text_2 } },
    tooltip: {
      backgroundColor: v.bg_card,
      borderColor: v.border_strong,
      textStyle: { color: v.text_1 }
    },
    xAxis: {
      axisLine: { lineStyle: { color: v.border_strong } },
      axisTick: { lineStyle: { color: v.border_strong } },
      axisLabel: { color: v.text_3 },
      splitLine: { lineStyle: { color: v.border } }
    },
    yAxis: {
      axisLine: { lineStyle: { color: v.border_strong } },
      axisTick: { lineStyle: { color: v.border_strong } },
      axisLabel: { color: v.text_3 },
      splitLine: { lineStyle: { color: v.border } }
    }
  }
}

let registered = false

export function registerChartThemes() {
  if (registered) return
  echarts.registerTheme('inspection-light', buildTheme('light'))
  echarts.registerTheme('inspection-dark', buildTheme('dark'))
  registered = true
}

/** 当前应使用的 ECharts 主题名（跟随 html data-theme） */
export function currentChartTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'inspection-dark' : 'inspection-light'
}
