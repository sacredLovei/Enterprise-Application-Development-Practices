<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import { formatTime } from '../utils/time'

const devices = ref([])
const filter = ref({ status: '', deviceType: '' })
const lastUpdated = ref('')
const msg = ref('')
let timer

async function load() {
  try {
    devices.value = await request.get('/devices')
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) {
    console.error(e)
  }
}

/** 手动上下线（S34）：经后端控制指令下发，15s 内状态翻转。 */
async function control(deviceId, action) {
  msg.value = ''
  try {
    await request.post(`/devices/${deviceId}/${action}`)
    msg.value = `${deviceId} 已下发${action === 'offline' ? '下线' : '上线'}指令（15 秒内生效）`
    setTimeout(load, 16000)
  } catch (e) {
    msg.value = '操作失败：' + (e.response?.data?.message || e.message)
  }
}

const filtered = () => devices.value.filter(d =>
  (!filter.value.status || d.status === filter.value.status) &&
  (!filter.value.deviceType || d.deviceType === filter.value.deviceType))

onMounted(() => {
  load()
  timer = setInterval(load, 5000)   // 台账 5s 刷新
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="card">
    <div class="form-row">
      <select v-model="filter.status">
        <option value="">全部状态</option>
        <option value="ONLINE">在线</option>
        <option value="OFFLINE">离线</option>
      </select>
      <select v-model="filter.deviceType">
        <option value="">全部类型</option>
        <option value="UAV">无人机</option>
        <option value="ROBOT_DOG">机器狗</option>
      </select>
      <button class="ghost" @click="load">手动刷新</button>
      <span class="hint">上次刷新：{{ lastUpdated || '—' }}（自动每 5 秒）</span>
    </div>
    <div v-if="msg" class="hint">{{ msg }}</div>
    <table class="grid">
      <thead>
        <tr><th>设备编号</th><th>类型</th><th>状态</th><th>电量</th><th>注册时间</th><th>最后心跳</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in filtered()" :key="d.deviceId">
          <td>{{ d.deviceId }}</td>
          <td>{{ d.deviceType === 'UAV' ? '无人机' : '机器狗' }}</td>
          <td><span class="badge" :class="d.status === 'ONLINE' ? 'online' : 'offline'">{{ d.status }}</span></td>
          <td>{{ d.battery }}%</td>
          <td>{{ formatTime(d.registerTime) }}</td>
          <td>{{ formatTime(d.lastHeartbeat) }}</td>
          <td>
            <button v-if="d.status === 'ONLINE'" class="ghost" @click="control(d.deviceId, 'offline')">手动下线</button>
            <button v-else class="ghost" @click="control(d.deviceId, 'online')">手动上线</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
