<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import { formatTime } from '../utils/time'

const form = ref({ alarmType: '', level: '', deviceId: '', keyword: '', from: 'now-24h', to: 'now' })
const devices = ref([])
const page = ref(0)
const size = 15
const result = ref({ total: 0, records: [] })
const detail = ref(null)   // S62：告警详情（含复核证据链）
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

// S62 证据链详情：Mongo 权威文档（含复核子文档），高空原图 + 机器狗红外复核图并列展示（TC032）
async function openDetail(alarmId) {
  try {
    detail.value = await request.get('/alarms/' + alarmId)
  } catch (e) {
    console.error('alarm detail failed', e)
  }
}

function statusText(s) {
  return { PENDING: '待复核', CONFIRMED: '确认属实', FALSE_ALARM: '误报', RESOLVED: '已处置' }[s] || s
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
        <tr><th>告警编号</th><th>类型</th><th>等级</th><th>设备</th><th>描述</th><th>时间</th><th>状态</th><th>证据链</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in result.records" :key="a.alarmId">
          <td>{{ a.alarmId }}</td>
          <td>{{ a.alarmType }}</td>
          <td><span class="badge" :class="a.level === 'CRITICAL' ? 'critical' : 'warn'">{{ a.level }}</span></td>
          <td>{{ a.deviceId }}</td>
          <td>{{ a.description }}</td>
          <td>{{ formatTime(a.occurredTime) }}</td>
          <td>
            <span class="badge" :class="{ ok: a.status === 'CONFIRMED', warn: a.status === 'FALSE_ALARM' }">{{ statusText(a.status) }}</span>
          </td>
          <td><button class="ghost small" @click="openDetail(a.alarmId)">详情</button></td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="ghost" :disabled="page <= 0" @click="goPage(page - 1)">上一页</button>
      <span>第 {{ page + 1 }} / {{ totalPages() }} 页（每页 {{ size }} 条）</span>
      <button class="ghost" :disabled="page + 1 >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>

  <!-- S62 证据链详情（TC032）：高空原图 + 机器狗红外复核图并列展示 -->
  <div v-if="detail" class="card">
    <div class="detail-head">
      <h3>告警详情：{{ detail.alarmId }}</h3>
      <button class="ghost small" @click="detail = null">关闭</button>
    </div>
    <div class="detail-meta">
      类型 {{ detail.alarmType }} · 等级 {{ detail.level }} · 设备 {{ detail.deviceId }} ·
      时间 {{ formatTime(detail.occurredTime) }} · 状态
      <span class="badge" :class="{ ok: detail.status === 'CONFIRMED', warn: detail.status === 'FALSE_ALARM' }">{{ statusText(detail.status) }}</span>
    </div>
    <p class="hint">{{ detail.description }}</p>
    <div class="evidence-row">
      <div class="evidence">
        <div class="evidence-title">① 高空发现原图（无人机）</div>
        <img :src="'/api/files/' + detail.alarmId" alt="高空证据图" class="evidence-img" />
      </div>
      <div class="evidence">
        <div class="evidence-title">② 机器狗红外复核图</div>
        <template v-if="detail.review && detail.review.imagePath">
          <img :src="'/api/files/' + detail.alarmId + '/review'" alt="红外复核图" class="evidence-img" />
          <div class="review-conclusion">
            复核结论：
            <span class="badge" :class="{ ok: detail.review.conclusion === 'CONFIRMED', warn: detail.review.conclusion === 'FALSE_ALARM' }">
              {{ statusText(detail.review.conclusion) }}
            </span>
            · 复核设备 {{ detail.review.reviewerDeviceId }}
            <span v-if="detail.review.reviewedAt"> · {{ formatTime(detail.review.reviewedAt) }}</span>
          </div>
          <div class="hint">{{ detail.review.note }}</div>
        </template>
        <div v-else class="hint" style="padding: 60px 0; text-align: center">
          尚未复核（自动派单进行中或该类型告警不派单）
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pager { display: flex; gap: 14px; align-items: center; justify-content: center; margin-top: 12px; font-size: 13px; color: #5c6b7a; }
button:disabled { opacity: .4; cursor: not-allowed; }
.small { font-size: 12px; padding: 3px 10px; }
.ok { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
.detail-head { display: flex; justify-content: space-between; align-items: center; }
.detail-meta { font-size: 13px; color: #5c6b7a; margin-bottom: 4px; }
.evidence-row { display: flex; gap: 20px; flex-wrap: wrap; }
.evidence { flex: 1 1 320px; }
.evidence-title { font-size: 13px; font-weight: 600; margin-bottom: 6px; color: #33414e; }
.evidence-img { width: 100%; border: 1px solid #e2e8ee; border-radius: 8px; display: block; }
.review-conclusion { margin-top: 8px; font-size: 13px; color: #33414e; }
</style>
