<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import DeviceMap from '../components/DeviceMap.vue'

const devices = ref([])
const alarms = ref([])
const tasks = ref([])
const summary = ref({})
const track = ref(null)   // S69：设备历史轨迹
let timer

async function load() {
  try {
    devices.value = await request.get('/devices')
    const r = await request.post('/search/alarms', { from: 'now-1h', to: 'now', page: 0, size: 50 })
    alarms.value = r.records || []
    const tp = await request.get('/tasks', { params: { page: 0, size: 50 } })
    tasks.value = tp.records || []
    const online = devices.value.filter(d => d.status === 'ONLINE').length
    const critical = alarms.value.filter(a => a.level === 'CRITICAL').length
    summary.value = { total: devices.value.length, online, alarmCount: alarms.value.length, critical }
  } catch (e) {
    console.error('overview load failed', e)
  }
}

// S69：请求设备轨迹并交由地图渲染
async function showTrack(deviceId) {
  try {
    track.value = await request.get('/devices/' + deviceId + '/track', { params: { minutes: 10 } })
  } catch (e) {
    console.error('track load failed', e)
  }
}

function clearTrack() {
  track.value = null
}

onMounted(() => {
  load()
  timer = setInterval(load, 3000)   // 总览 3s 轮询（设计报告 5.2.7 刷新策略）
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="cards">
    <div class="stat"><div class="num">{{ summary.total ?? '-' }}</div><div class="label">设备总数</div></div>
    <div class="stat"><div class="num green">{{ summary.online ?? '-' }}</div><div class="label">在线</div></div>
    <div class="stat"><div class="num orange">{{ summary.alarmCount ?? '-' }}</div><div class="label">近 1 小时告警</div></div>
    <div class="stat"><div class="num red">{{ summary.critical ?? '-' }}</div><div class="label">严重告警</div></div>
  </div>
  <div class="card">
    <h3>园区设备与告警分布（每 3 秒刷新）</h3>
    <DeviceMap :devices="devices" :alarms="alarms" :tasks="tasks" :track="track"
               @track-requested="showTrack" />
    <div class="hint">
      蓝色 ✈ 无人机 / 绿色 🐕 机器狗 / 红色 ! 告警点 / 🎯 任务目标点。底图为 OpenStreetMap。
      点击设备图标 → 「📈 最近 10 分钟轨迹」查看回放。
      <button v-if="track" class="ghost small" style="margin-left:10px" @click="clearTrack">清除轨迹</button>
    </div>
  </div>
</template>

<style scoped>
.cards { display: flex; gap: 14px; margin-bottom: 16px; }
.stat {
  flex: 1; background: #fff; border-radius: 10px; padding: 14px;
  text-align: center; box-shadow: 0 1px 4px rgba(20,33,46,.08);
}
.num { font-size: 26px; font-weight: 700; }
.num.green { color: #1a8a4a; }
.num.orange { color: #b97a12; }
.num.red { color: #c0392b; }
.label { color: #7b8a99; font-size: 12px; margin-top: 4px; }
</style>
