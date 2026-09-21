<script setup>
// S99-b：告警列表子页（母项）——满幅检索 + 表格，详情跳独立子路由
// 业务逻辑与原 Alarms.vue 列表部分一致（防抖检索 / 5s 自动刷新 / 竞态守卫 / 深分页防护）
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'
import { formatTime } from '../utils/time'

const router = useRouter()

const form = ref({ alarmType: '', level: '', deviceId: '', keyword: '', from: 'now-24h', to: 'now' })
const devices = ref([])
const page = ref(0)
const size = 15
const result = ref({ total: 0, records: [] })
let timer, debounceTimer, deviceTimer
let seq = 0   // 请求序号：丢弃过期响应，防止自动刷新与手动检索竞态覆盖新结果

async function loadDevices() {
  try {
    devices.value = await request.get('/devices')
  } catch (e) {
    console.error('load devices failed', e)
  }
}

async function search() {
  const my = ++seq
  try {
    const data = await request.post('/search/alarms', {
      alarmType: form.value.alarmType || null,
      level: form.value.level || null,
      deviceId: form.value.deviceId || null,
      keyword: form.value.keyword || null,
      from: form.value.from,
      to: form.value.to,
      page: page.value,
      size
    })
    if (my === seq) result.value = data   // 仅应用最新请求的结果
  } catch (e) {
    console.error(e)
  }
}

function statusText(s) {
  return { PENDING: '待复核', CONFIRMED: '确认属实', FALSE_ALARM: '误报', RESOLVED: '已处置' }[s] || s
}

// 输入防抖：停止输入 400ms 后自动检索
function onFilterInput() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; search() }, 400)
}

function goPage(p) {
  page.value = Math.max(0, p)
  search()
}

const totalPages = () => {
  // S71 深分页防护：ES 仅可翻前 10,000 条，页码按上限截断
  const maxPages = Math.floor(10000 / size)
  return Math.max(1, Math.min(Math.ceil(result.value.total / size), maxPages))
}

const depthCapped = () => result.value.total > 10000

function openDetail(alarmId) {
  router.push('/alarms/detail/' + alarmId)
}

onMounted(() => {
  loadDevices()
  search()
  timer = setInterval(search, 5000)          // 自动刷新沿用当前筛选与页码
  deviceTimer = setInterval(loadDevices, 30000)
})
onUnmounted(() => { clearInterval(timer); clearTimeout(debounceTimer); clearInterval(deviceTimer) })
</script>

<template>
  <div class="card">
    <div class="list-head">
      <h3>告警检索</h3>
      <span class="hint" style="margin:0">MongoDB 权威 + Elasticsearch 检索副本 · 每 5 秒自动刷新 · 筛选即输即查</span>
    </div>
    <div class="form-row">
      <select v-model="form.alarmType" @change="onFilterInput">
        <option value="">全部类型</option>
        <option value="PERIMETER_BREACH">周界入侵</option>
        <option value="DEVICE_OVERHEAT">设备过热</option>
        <option value="BATTERY_LOW">电量不足</option>
        <option value="DEVICE_OFFLINE">设备离线</option>
      </select>
      <select v-model="form.level" @change="onFilterInput">
        <option value="">全部等级</option>
        <option value="CRITICAL">严重</option>
        <option value="WARN">警告</option>
      </select>
      <select v-model="form.deviceId" @change="onFilterInput">
        <option value="">全部设备</option>
        <option v-for="d in devices" :key="d.deviceId" :value="d.deviceId">
          {{ d.deviceId }}（{{ d.deviceType === 'UAV' ? '无人机' : '机器狗' }}·{{ d.status }}）
        </option>
      </select>
      <input v-model="form.keyword" placeholder="关键词" style="width:160px" @input="onFilterInput" />
      <button @click="page = 0; search()">检索</button>
      <span class="hint" style="margin:0 0 0 auto">
        共 {{ result.total }} 条（{{ form.from }} ~ {{ form.to }}）
        <span v-if="depthCapped()" class="warn-hint">⚠ 仅可浏览前 10,000 条，请缩小范围</span>
      </span>
    </div>
  </div>

  <div class="card">
    <table class="grid">
      <thead>
        <tr><th>告警编号</th><th>类型</th><th>等级</th><th>设备</th><th>描述</th><th>时间</th><th>状态</th><th>证据链</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in result.records" :key="a.alarmId" class="row-click" @click="openDetail(a.alarmId)">
          <td class="mono">{{ a.alarmId }}</td>
          <td>{{ a.alarmType }}</td>
          <td><span class="badge" :class="a.level === 'CRITICAL' ? 'critical' : 'warn'">{{ a.level }}</span></td>
          <td>{{ a.deviceId }}</td>
          <td class="desc" :title="a.description">{{ a.description }}</td>
          <td>{{ formatTime(a.occurredTime) }}</td>
          <td>
            <span class="badge" :class="{ ok: a.status === 'CONFIRMED', warn: a.status === 'FALSE_ALARM' }">{{ statusText(a.status) }}</span>
          </td>
          <td><button class="ghost small" @click.stop="openDetail(a.alarmId)">详情</button></td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="ghost" :disabled="page <= 0" @click="goPage(page - 1)">上一页</button>
      <span>第 {{ page + 1 }} / {{ totalPages() }} 页（每页 {{ size }} 条）</span>
      <button class="ghost" :disabled="page + 1 >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.list-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.mono { font-family: var(--font-mono); font-size: 12px; }
.desc { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-click { cursor: pointer; }
.pager { display: flex; gap: 14px; align-items: center; justify-content: center; margin-top: 12px; font-size: 13px; color: var(--text-2); }
.warn-hint { color: var(--warn); margin-left: 8px; }
</style>
