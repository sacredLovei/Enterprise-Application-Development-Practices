<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '../api/request'

const pieEl = ref(null)
const trendEl = ref(null)
const summary = ref({})
let pie, trend, timer

async function load() {
  try {
    const s = await request.get('/search/stats')
    summary.value = s
    renderPie(s.by_type || [])
    renderTrend(s.trend || [])
  } catch (e) {
    console.error(e)
  }
}

function renderPie(buckets) {
  if (!pieEl.value) return
  if (!pie) pie = echarts.init(pieEl.value)
  pie.setOption({
    title: { text: '近 24 小时告警类型分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['35%', '65%'],
      data: buckets.map(b => ({ name: b.key, value: b.count }))
    }]
  })
}

function renderTrend(buckets) {
  if (!trendEl.value) return
  if (!trend) trend = echarts.init(trendEl.value)
  trend.setOption({
    title: { text: '告警按小时趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: buckets.map(b => axisTime(b.time)),
      axisLabel: { fontSize: 11 }
    },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: buckets.map(b => b.count), itemStyle: { color: '#2f6fed' } }]
  })
}

/** 时间轴标签：ISO → "MM-dd HH:00" 北京时间（用户反馈：原样显示 ISO 太长） */
function axisTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const parts = d.toLocaleString('zh-CN', { hour12: false, timeZone: 'Asia/Shanghai' })
  const m = parts.match(/(\d+)\/(\d+)\/(\d+) (\d+):\d+:\d+/)
  if (m) return `${m[1]}-${m[2].padStart(2, '0')} ${m[4]}:00`
  return parts.slice(5, 16)
}

onMounted(() => {
  load()
  timer = setInterval(load, 30000)   // 统计 30s 刷新（设计报告 5.2.7）
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="card">
    <h3>近 24 小时概况（每 30 秒刷新）</h3>
    <div class="hint">数据源：Elasticsearch 聚合（Kibana 另有运维级仪表盘，经 /kibana/ 访问）</div>
    <div ref="pieEl" class="chart"></div>
  </div>
  <div class="card">
    <div ref="trendEl" class="chart"></div>
  </div>
</template>

<style scoped>
.chart { height: 340px; width: 100%; }
</style>
