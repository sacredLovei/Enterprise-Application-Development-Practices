<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import { formatTime } from '../utils/time'

const form = ref({ alarmType: '', level: '', deviceId: '', keyword: '', from: 'now-24h', to: 'now' })
const devices = ref([])
const page = ref(0)
const size = 15
const result = ref({ total: 0, records: [] })
let timer, debounceTimer, deviceTimer
let seq = 0   // 请求序号：丢弃过期响应，防止自动刷新与手动检索竞态覆盖新结果

// 设备下拉选项：动态取自设备台账（/api/devices），增删设备自动更新，无硬编码
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

// 输入防抖：停止输入 400ms 后自动检索（体验即时，不必每次点按钮）
function onFilterInput() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; search() }, 400)
}

function goPage(p) {
  page.value = Math.max(0, p)
  search()
}

const totalPages = () => Math.max(1, Math.ceil(result.value.total / size))

onMounted(() => {
  loadDevices()
  search()
  timer = setInterval(search, 5000)          // 自动刷新沿用当前筛选与页码
  deviceTimer = setInterval(loadDevices, 30000)   // 设备清单 30s 刷新
})
onUnmounted(() => { clearInterval(timer); clearTimeout(debounceTimer); clearInterval(deviceTimer) })
</script>

<template>
  <div class="card">
    <h3>告警检索（MongoDB 权威 + Elasticsearch 检索副本）</h3>
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
      <input v-model="form.keyword" placeholder="关键词" style="width:140px" @input="onFilterInput" />
      <button @click="page = 0; search()">检索</button>
    </div>
    <div class="hint">共 {{ result.total }} 条（时间 {{ form.from }} ~ {{ form.to }}，每 5 秒自动刷新，筛选即输即查）</div>
  </div>

  <div class="card">
    <table class="grid">
      <thead>
        <tr><th>告警编号</th><th>类型</th><th>等级</th><th>设备</th><th>描述</th><th>时间</th><th>证据</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in result.records" :key="a.alarmId">
          <td>{{ a.alarmId }}</td>
          <td>{{ a.alarmType }}</td>
          <td><span class="badge" :class="a.level === 'CRITICAL' ? 'critical' : 'warn'">{{ a.level }}</span></td>
          <td>{{ a.deviceId }}</td>
          <td>{{ a.description }}</td>
          <td>{{ formatTime(a.occurredTime) }}</td>
          <td>{{ a.snapshotPath ? 'HDFS ✓' : '—' }}</td>
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
.pager { display: flex; gap: 14px; align-items: center; justify-content: center; margin-top: 12px; font-size: 13px; color: #5c6b7a; }
button:disabled { opacity: .4; cursor: not-allowed; }
</style>
