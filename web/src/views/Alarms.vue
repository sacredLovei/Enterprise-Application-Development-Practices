<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'

const form = ref({ alarmType: '', level: '', deviceId: '', keyword: '', from: 'now-24h', to: 'now' })
const result = ref({ total: 0, records: [] })
let timer

async function search() {
  try {
    result.value = await request.post('/search/alarms', {
      alarmType: form.value.alarmType || null,
      level: form.value.level || null,
      deviceId: form.value.deviceId || null,
      keyword: form.value.keyword || null,
      from: form.value.from,
      to: form.value.to,
      page: 0,
      size: 50
    })
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  search()
  // 结果按当前筛选条件每 5 秒自动刷新（筛选条件是用户设定的，不会被覆盖，设计报告 5.2.7）
  timer = setInterval(search, 5000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="card">
    <h3>告警检索（MongoDB 权威 + Elasticsearch 检索副本）</h3>
    <div class="form-row">
      <select v-model="form.alarmType">
        <option value="">全部类型</option>
        <option value="PERIMETER_BREACH">周界入侵</option>
        <option value="DEVICE_OVERHEAT">设备过热</option>
        <option value="BATTERY_LOW">电量不足</option>
        <option value="DEVICE_OFFLINE">设备离线</option>
      </select>
      <select v-model="form.level">
        <option value="">全部等级</option>
        <option value="CRITICAL">严重</option>
        <option value="WARN">警告</option>
      </select>
      <input v-model="form.deviceId" placeholder="设备编号" style="width:140px" />
      <input v-model="form.keyword" placeholder="关键词" style="width:140px" />
      <button @click="search">检索</button>
    </div>
    <div class="hint">共 {{ result.total }} 条（时间范围 {{ form.from }} ~ {{ form.to }}，每 5 秒自动刷新）</div>
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
          <td>{{ (a.occurredTime || '').replace('T', ' ').slice(0, 19) }}</td>
          <td>{{ a.snapshotPath ? 'HDFS ✓' : '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
