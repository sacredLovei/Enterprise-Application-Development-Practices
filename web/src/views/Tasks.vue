<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import TargetPicker from '../components/TargetPicker.vue'
import { formatTime } from '../utils/time'

const form = ref({ taskType: 'POINT_REVIEW', deviceId: '', priority: 2, remark: '' })
const target = ref(null)   // { lng, lat } 地图点选
const page = ref(0)
const size = 15
const result = ref({ total: 0, records: [] })
const devices = ref([])
const msg = ref('')
let timer, deviceTimer

const needTarget = computed(() => ['POINT_REVIEW', 'AREA_COVER'].includes(form.value.taskType))
const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / size)))

const TYPE_LABEL = {
  PERIMETER_PATROL: '周界巡逻',
  AREA_COVER: '区域覆盖',
  POINT_REVIEW: '定点复核',
  RETURN_HOME: '返航'
}

async function load() {
  try {
    result.value = await request.get('/tasks', { params: { page: page.value, size } })
  } catch (e) {
    console.error(e)
  }
}

async function loadDevices() {
  try {
    devices.value = await request.get('/devices')
    if (!form.value.deviceId && devices.value.length) {
      form.value.deviceId = devices.value.find(d => d.status === 'ONLINE')?.deviceId || ''
    }
  } catch (e) {
    console.error('load devices failed', e)
  }
}

function onTaskTypeChange() {
  target.value = null
}

function goPage(p) {
  page.value = Math.max(0, p)
  load()
}

async function submit() {
  msg.value = ''
  if (!form.value.deviceId) {
    msg.value = '请选择执行设备'
    return
  }
  if (needTarget.value && !target.value) {
    msg.value = '请在地图上点选目标位置'
    return
  }
  try {
    const t = await request.post('/tasks', {
      taskType: form.value.taskType,
      deviceId: form.value.deviceId,
      priority: Number(form.value.priority),
      remark: form.value.remark,
      targetLng: needTarget.value ? target.value.lng : null,
      targetLat: needTarget.value ? target.value.lat : null
    })
    msg.value = `任务已下发：${t.taskId}`
    target.value = null
    page.value = 0
    load()
  } catch (e) {
    msg.value = '下发失败：' + (e.response?.data?.message || e.message)
  }
}

async function cancel(taskId) {
  try {
    await request.post(`/tasks/${taskId}/cancel`)
    load()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  load()
  loadDevices()
  timer = setInterval(load, 10000)               // 任务列表 10s 刷新（设计报告 5.2.7）
  deviceTimer = setInterval(loadDevices, 30000)  // 设备清单 30s 刷新
})
onUnmounted(() => { clearInterval(timer); clearInterval(deviceTimer) })
</script>

<template>
  <div class="card">
    <h3>下发巡检任务（指令经 Kafka → 设备真实执行回执）</h3>
    <div class="form-row">
      <select v-model="form.taskType" @change="onTaskTypeChange">
        <option value="PERIMETER_PATROL">周界巡逻</option>
        <option value="AREA_COVER">区域覆盖（需选地点）</option>
        <option value="POINT_REVIEW">定点复核（需选地点）</option>
        <option value="RETURN_HOME">返航</option>
      </select>
      <select v-model="form.deviceId" style="min-width:220px">
        <option v-for="d in devices" :key="d.deviceId" :value="d.deviceId">
          {{ d.deviceId }}（{{ d.deviceType === 'UAV' ? '无人机' : '机器狗' }}·{{ d.status }}·电量{{ d.battery }}%）
        </option>
      </select>
      <select v-model="form.priority">
        <option :value="1">优先级 1（高）</option>
        <option :value="2">优先级 2（中）</option>
        <option :value="3">优先级 3（低）</option>
      </select>
      <input v-model="form.remark" placeholder="备注" style="width:180px" />
      <button @click="submit">下发任务</button>
    </div>
    <div v-if="needTarget" class="card inner">
      <h3>目标位置（点击地图选点；机器狗沿地面路网最短路径前往，无人机直线飞行）</h3>
      <TargetPicker v-model="target" />
    </div>
    <div v-if="msg" class="hint">{{ msg }}</div>
  </div>

  <div class="card">
    <h3>任务列表（每 10 秒刷新，最新在前）</h3>
    <table class="grid">
      <thead>
        <tr><th>任务编号</th><th>类型</th><th>执行设备</th><th>优先级</th><th>状态</th><th>创建时间</th><th>完成时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="t in result.records" :key="t.taskId">
          <td class="mono" :title="t.taskId">{{ t.taskId }}</td>
          <td>{{ TYPE_LABEL[t.taskType] || t.taskType }}</td>
          <td>{{ t.deviceId }}</td>
          <td>{{ t.priority }}</td>
          <td><span class="badge" :class="'st-' + (t.status || '').toLowerCase()">{{ t.status }}</span></td>
          <td>{{ formatTime(t.createTime) }}</td>
          <td>{{ t.status === 'DONE' || t.status === 'FAILED' || t.status === 'CANCELLED' ? formatTime(t.finishTime) : '—' }}</td>
          <td><button v-if="t.status === 'DISPATCHED' || t.status === 'RUNNING'" class="ghost" @click="cancel(t.taskId)">取消</button></td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="ghost" :disabled="page <= 0" @click="goPage(page - 1)">上一页</button>
      <span>第 {{ page + 1 }} / {{ totalPages }} 页（每页 {{ size }} 条，共 {{ result.total }} 条）</span>
      <button class="ghost" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.inner { background: #f8fafc; border: 1px dashed #c9d4df; }
.pager { display: flex; gap: 14px; align-items: center; justify-content: center; margin-top: 12px; font-size: 13px; color: #5c6b7a; }
button:disabled { opacity: .4; cursor: not-allowed; }
.mono { font-family: Consolas, monospace; font-size: 12px; }
.badge.st-dispatched { background: #e8effd; color: #2f5fb3; }
.badge.st-running { background: #fdf3e0; color: #b97a12; }
.badge.st-done { background: #e3f7ec; color: #1a8a4a; }
.badge.st-cancelled { background: #eef2f7; color: #7b8a99; }
.badge.st-failed { background: #fdeaea; color: #c0392b; }
</style>
