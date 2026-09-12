<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'

const devices = ref([])
const filter = ref({ status: '', deviceType: '' })
let timer

async function load() {
  try {
    devices.value = await request.get('/devices')
  } catch (e) {
    console.error(e)
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
    </div>
    <table class="grid">
      <thead>
        <tr><th>设备编号</th><th>类型</th><th>状态</th><th>电量</th><th>注册时间</th><th>最后心跳</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in filtered()" :key="d.deviceId">
          <td>{{ d.deviceId }}</td>
          <td>{{ d.deviceType === 'UAV' ? '无人机' : '机器狗' }}</td>
          <td><span class="badge" :class="d.status === 'ONLINE' ? 'online' : 'offline'">{{ d.status }}</span></td>
          <td>{{ d.battery }}%</td>
          <td>{{ (d.registerTime || '').replace('T', ' ').slice(0, 19) }}</td>
          <td>{{ (d.lastHeartbeat || '').replace('T', ' ').slice(0, 19) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
