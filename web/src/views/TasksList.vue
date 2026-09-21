<script setup>
// S99-a：任务列表子页——满幅表格 + 日志时间轴（业务逻辑与原 Tasks.vue 列表部分一致）
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import { formatTime } from '../utils/time'

const page = ref(0)
const size = 15
const result = ref({ total: 0, records: [] })
const logTask = ref(null)   // S85：日志面板当前任务
const logs = ref([])
let timer

const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / size)))

const TYPE_LABEL = {
  PERIMETER_PATROL: '周界巡逻',
  AREA_COVER: '区域覆盖',
  POINT_REVIEW: '定点复核',
  RETURN_HOME: '返航'
}

const ACTION_LABEL = {
  COMMAND_RECEIVED: '指令接收',
  EXECUTING: '执行中',
  DONE: '任务完成',
  CANCELLED: '任务取消',
  FAILED: '执行失败'
}

async function load() {
  try {
    result.value = await request.get('/tasks', { params: { page: page.value, size } })
  } catch (e) {
    console.error(e)
  }
}

function goPage(p) {
  page.value = Math.max(0, p)
  load()
}

async function cancel(taskId) {
  try {
    await request.post(`/tasks/${taskId}/cancel`)
    load()
  } catch (e) {
    console.error(e)
  }
}

async function openLogs(t) {
  logTask.value = t
  try {
    logs.value = await request.get(`/tasks/${t.taskId}/logs`)
  } catch (e) {
    logs.value = []
    console.error('load task logs failed', e)
  }
}

function closeLogs() {
  logTask.value = null
  logs.value = []
}

onMounted(() => {
  load()
  timer = setInterval(load, 10000)   // 任务列表 10s 刷新（设计报告 5.2.7）
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="card">
    <div class="list-head">
      <h3>任务列表</h3>
      <span class="hint" style="margin:0">每 10 秒自动刷新，最新在前</span>
    </div>
    <table class="grid">
      <thead>
        <tr><th>任务编号</th><th>类型</th><th>执行设备</th><th>优先级</th><th>状态</th><th>备注</th><th>创建时间</th><th>完成时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="t in result.records" :key="t.taskId">
          <td class="mono" :title="t.taskId">{{ t.taskId }}</td>
          <td>{{ TYPE_LABEL[t.taskType] || t.taskType }}</td>
          <td>{{ t.deviceId }}</td>
          <td>{{ t.priority }}</td>
          <td><span class="badge" :class="'st-' + (t.status || '').toLowerCase()">{{ t.status }}</span></td>
          <td class="remark" :title="t.remark">{{ t.remark || '—' }}</td>
          <td>{{ formatTime(t.createTime) }}</td>
          <td>{{ t.status === 'DONE' || t.status === 'FAILED' || t.status === 'CANCELLED' ? formatTime(t.finishTime) : '—' }}</td>
          <td>
            <button class="ghost small" @click="openLogs(t)">日志</button>
            <button v-if="t.status === 'DISPATCHED' || t.status === 'RUNNING'" class="ghost small" style="margin-left:6px" @click="cancel(t.taskId)">取消</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="ghost" :disabled="page <= 0" @click="goPage(page - 1)">上一页</button>
      <span>第 {{ page + 1 }} / {{ totalPages }} 页（每页 {{ size }} 条，共 {{ result.total }} 条）</span>
      <button class="ghost" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>

  <!-- S85：任务执行日志时间轴（归档回执完整追溯） -->
  <div v-if="logTask" class="card">
    <div class="detail-head">
      <h3>执行日志：{{ logTask.taskId }}</h3>
      <button class="ghost small" @click="closeLogs">关闭</button>
    </div>
    <div class="hint">
      设备 {{ logTask.deviceId }} · 类型 {{ TYPE_LABEL[logTask.taskType] || logTask.taskType }} ·
      状态 <span class="badge" :class="'st-' + (logTask.status || '').toLowerCase()">{{ logTask.status }}</span> ·
      备注 {{ logTask.remark || '—' }} · 共 {{ logs.length }} 条回执
    </div>
    <ol class="timeline">
      <li v-for="(l, i) in logs" :key="i">
        <span class="tl-time">{{ formatTime(l.ts) }}</span>
        <span class="tl-action">{{ ACTION_LABEL[l.action] || l.action }}</span>
        <span class="tl-dev">{{ l.deviceId }}</span>
      </li>
    </ol>
    <div v-if="logs.length === 0" class="hint">暂无归档日志（任务下发后由设备回执产生）</div>
  </div>
</template>

<style scoped>
.list-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.pager { display: flex; gap: 14px; align-items: center; justify-content: center; margin-top: 12px; font-size: 13px; color: var(--text-2); }
button:disabled { opacity: .4; cursor: not-allowed; }
.mono { font-family: var(--font-mono); font-size: 12px; }
.remark { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge.st-dispatched { background: var(--info-soft); color: var(--info); }
.badge.st-running { background: var(--warn-soft); color: var(--warn); }
.badge.st-done { background: var(--ok-soft); color: var(--ok); }
.badge.st-cancelled { background: var(--neutral-soft); color: var(--text-3); }
.badge.st-failed { background: var(--danger-soft); color: var(--danger); }
.detail-head { display: flex; justify-content: space-between; align-items: center; }
.timeline { list-style: none; padding: 0; margin: 10px 0 0; }
.timeline li { display: flex; gap: 14px; align-items: center; padding: 7px 10px; border-left: 3px solid var(--border-strong); margin-bottom: 6px; background: var(--bg-inset); border-radius: 0 6px 6px 0; font-size: 13px; }
.tl-time { color: var(--text-3); font-family: var(--font-mono); font-size: 12px; min-width: 150px; }
.tl-action { font-weight: 600; color: var(--text-1); }
.tl-dev { color: var(--text-2); font-size: 12px; }
</style>
