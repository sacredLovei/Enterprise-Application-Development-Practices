<script setup>
// S99-a：下发任务子页——左侧表单 + 右侧大地图（满幅，TDesign 式栅格）
// 业务逻辑与原 Tasks.vue 表单完全一致，仅排版重构
// S100：反馈强化——按钮 loading、Toast 通知、下发后实时状态卡（轮询至终态）
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import TargetPicker from '../components/TargetPicker.vue'
import { toast } from '../utils/toast'

const form = ref({ taskType: 'POINT_REVIEW', deviceId: '', priority: 2, remark: '' })
const target = ref(null)   // { lng, lat } 地图点选
const devices = ref([])
const msg = ref('')
const submitting = ref(false)          // S100：提交 loading 态
const lastTask = ref(null)             // S100：最近下发任务（状态卡）
let deviceTimer, statusTimer

const needTarget = computed(() => ['POINT_REVIEW', 'AREA_COVER'].includes(form.value.taskType))

const TYPE_LABEL = {
  PERIMETER_PATROL: '周界巡逻',
  AREA_COVER: '区域覆盖',
  POINT_REVIEW: '定点复核',
  RETURN_HOME: '返航'
}

const STATUS_LABEL = {
  DISPATCHED: '已下发 · 等待设备接单',
  RUNNING: '执行中 · 设备正在执行',
  DONE: '已完成',
  FAILED: '执行失败',
  CANCELLED: '已取消'
}
const TERMINAL = ['DONE', 'FAILED', 'CANCELLED']

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

// S100：轮询最近下发任务的状态，状态卡实时展示流转，终态自动停止
function watchTask(taskId) {
  clearInterval(statusTimer)
  statusTimer = setInterval(async () => {
    try {
      const r = await request.get('/tasks', { params: { page: 0, size: 50 } })
      const t = (r.records || []).find(x => x.taskId === taskId)
      if (!t) return
      if (lastTask.value) {
        lastTask.value.status = t.status
        lastTask.value.finishTime = t.finishTime
      }
      if (TERMINAL.includes(t.status)) {
        clearInterval(statusTimer)
        if (t.status === 'DONE') toast.success(`任务已完成：${TYPE_LABEL[t.taskType] || t.taskType}`)
        else if (t.status === 'FAILED') toast.error('任务执行失败，可在任务列表查看日志')
      }
    } catch (e) {
      console.error('watch task failed', e)
    }
  }, 2500)
}

async function submit() {
  msg.value = ''
  if (!form.value.deviceId) {
    msg.value = '请选择执行设备'
    toast.error('请选择执行设备')
    return
  }
  if (needTarget.value && !target.value) {
    msg.value = '请在地图上点选目标位置'
    toast.error('请在地图上点选目标位置')
    return
  }
  submitting.value = true
  try {
    const t = await request.post('/tasks', {
      taskType: form.value.taskType,
      deviceId: form.value.deviceId,
      priority: Number(form.value.priority),
      remark: form.value.remark,
      targetLng: needTarget.value ? target.value.lng : null,
      targetLat: needTarget.value ? target.value.lat : null
    })
    toast.success(`任务已下发：${t.taskId}（${TYPE_LABEL[t.taskType] || t.taskType} → ${t.deviceId}）`)
    lastTask.value = { ...t, status: t.status || 'DISPATCHED' }
    msg.value = ''
    watchTask(t.taskId)
  } catch (e) {
    const m = '下发失败：' + (e.response?.data?.message || e.message)
    msg.value = m
    toast.error(m)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDevices()
  deviceTimer = setInterval(loadDevices, 30000)  // 设备清单 30s 刷新
})
onUnmounted(() => {
  clearInterval(deviceTimer)
  clearInterval(statusTimer)
})
</script>

<template>
  <div class="dispatch-grid">
    <!-- 左：任务表单（窄列） -->
    <div class="card form-card">
      <h3>下发巡检任务</h3>
      <div class="hint" style="margin:0 0 12px">指令经 Kafka → 设备真实执行回执</div>

      <label class="field">
        <span>任务类型</span>
        <select v-model="form.taskType" @change="onTaskTypeChange">
          <option value="PERIMETER_PATROL">周界巡逻</option>
          <option value="AREA_COVER">区域覆盖（需选地点）</option>
          <option value="POINT_REVIEW">定点复核（需选地点）</option>
          <option value="RETURN_HOME">返航</option>
        </select>
      </label>
      <label class="field">
        <span>执行设备</span>
        <select v-model="form.deviceId">
          <option v-for="d in devices" :key="d.deviceId" :value="d.deviceId">
            {{ d.deviceId }}（{{ d.deviceType === 'UAV' ? '无人机' : '机器狗' }}·{{ d.status }}·电量{{ d.battery }}%）
          </option>
        </select>
      </label>
      <label class="field">
        <span>优先级</span>
        <select v-model="form.priority">
          <option :value="1">优先级 1（高）</option>
          <option :value="2">优先级 2（中）</option>
          <option :value="3">优先级 3（低）</option>
        </select>
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model="form.remark" placeholder="备注（可选）" />
      </label>

      <button class="submit-btn" :disabled="submitting" @click="submit">
        {{ submitting ? '下发中…' : '下发任务' }}
      </button>
      <div v-if="msg" class="hint">{{ msg }}</div>

      <!-- S100：最近下发任务实时状态卡 -->
      <div v-if="lastTask" class="task-status" :class="'ts-' + (lastTask.status || '').toLowerCase()">
        <div class="ts-row">
          <span class="badge" :class="'st-' + (lastTask.status || '').toLowerCase()">{{ lastTask.status }}</span>
          <span class="ts-label">{{ STATUS_LABEL[lastTask.status] || lastTask.status }}</span>
        </div>
        <div class="ts-task mono">{{ lastTask.taskId }}</div>
        <div class="ts-meta">
          {{ TYPE_LABEL[lastTask.taskType] || lastTask.taskType }} → {{ lastTask.deviceId }} · 优先级 {{ lastTask.priority }}
          <span v-if="lastTask.targetLng != null"> · 目标 ({{ lastTask.targetLng }}, {{ lastTask.targetLat }})</span>
        </div>
      </div>
    </div>

    <!-- 右：大地图选点（满幅填充） -->
    <div class="card map-card">
      <h3>目标位置</h3>
      <div class="hint" style="margin:0 0 8px">
        点击地图选点；机器狗沿地面路网最短路径前往，无人机直线飞行
      </div>
      <TargetPicker v-if="needTarget" v-model="target" height="min(62vh, 640px)" />
      <div v-else class="no-target">
        <p>当前任务类型无需选取目标位置</p>
        <p class="hint">周界巡逻 = 环园一圈 · 返航 = 回到基地</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dispatch-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}
.form-card { display: flex; flex-direction: column; }
.field { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--text-2); margin-bottom: 12px; }
.field input, .field select {
  padding: 9px 10px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-1);
  font-size: 13px;
  background: var(--bg-card);
  color: var(--text-1);
}
.submit-btn { margin-top: 4px; padding: 11px; font-size: 14px; font-weight: 600; }
.submit-btn:disabled { opacity: .6; cursor: not-allowed; }

/* S100：最近下发任务实时状态卡 */
.task-status {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid var(--border);
  border-left: 3px solid var(--info);
  border-radius: var(--radius-1);
  background: var(--bg-inset);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.task-status.ts-running { border-left-color: var(--warn); }
.task-status.ts-done { border-left-color: var(--ok); }
.task-status.ts-failed, .task-status.ts-cancelled { border-left-color: var(--danger); }
.ts-row { display: flex; align-items: center; gap: 8px; }
.ts-label { font-size: 13px; color: var(--text-1); font-weight: 600; }
.ts-task { font-size: 11px; color: var(--text-3); word-break: break-all; }
.ts-meta { font-size: 12px; color: var(--text-2); }
.badge.st-dispatched { background: var(--info-soft); color: var(--info); }
.badge.st-running { background: var(--warn-soft); color: var(--warn); }
.badge.st-done { background: var(--ok-soft); color: var(--ok); }
.badge.st-cancelled { background: var(--neutral-soft); color: var(--text-3); }
.badge.st-failed { background: var(--danger-soft); color: var(--danger); }
.map-card { display: flex; flex-direction: column; }
.no-target {
  flex: 1;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-1);
  color: var(--text-2);
  font-size: 14px;
  gap: 4px;
}
</style>
