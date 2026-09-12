<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import TargetPicker from '../components/TargetPicker.vue'

const form = ref({ taskType: 'POINT_REVIEW', deviceId: '', priority: 2, remark: '' })
const target = ref(null)   // { lng, lat } 地图点选
const tasks = ref([])
const devices = ref([])
const msg = ref('')
let timer, deviceTimer

const needTarget = computed(() => ['POINT_REVIEW', 'AREA_COVER'].includes(form.value.taskType))

async function load() {
  try {
    tasks.value = await request.get('/tasks')
  } catch (e) {
    console.error(e)
  }
}

// 设备下拉选项：动态取自设备台账，增删设备自动更新
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
  target.value = null   // 切换任务类型清空已选目标
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
    <div v-if="needTarget" class="card">
      <h3>目标位置（点击地图选点；机器狗将沿地面路网最短路径前往，无人机直线飞行）</h3>
      <TargetPicker v-model="target" />
    </div>
    <div v-if="msg" class="hint">{{ msg }}</div>
  </div>

  <div class="card">
    <h3>任务列表（每 10 秒刷新）</h3>
    <table class="grid">
      <thead>
        <tr><th>任务编号</th><th>类型</th><th>执行设备</th><th>优先级</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="t in tasks" :key="t.taskId">
          <td>{{ t.taskId }}</td>
          <td>{{ t.taskType }}</td>
          <td>{{ t.deviceId }}</td>
          <td>{{ t.priority }}</td>
          <td><span class="badge pending">{{ t.status }}</span></td>
          <td>{{ (t.createTime || '').replace('T', ' ').slice(0, 19) }}</td>
          <td><button v-if="t.status === 'DISPATCHED'" class="ghost" @click="cancel(t.taskId)">取消</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
