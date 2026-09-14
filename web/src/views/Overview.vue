<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import DeviceMap from '../components/DeviceMap.vue'

const devices = ref([])
const alarms = ref([])
const tasks = ref([])
const summary = ref({})
const track = ref(null)   // S69：设备历史轨迹
const health = ref(null)  // S80：系统健康（Mongo/ES/HDFS/Kafka LAG）
let timer, healthTimer

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

// S80：系统健康（10 秒轮询）
async function loadHealth() {
  try {
    health.value = await request.get('/system/health')
  } catch (e) {
    console.error('health load failed', e)
  }
}

function up(v) { return v === 'up' || v === 'green' }

onMounted(() => {
  load()
  loadHealth()
  timer = setInterval(load, 3000)          // 总览 3s 轮询（设计报告 5.2.7 刷新策略）
  healthTimer = setInterval(loadHealth, 10000)   // 健康面板 10s 轮询
})
onUnmounted(() => { clearInterval(timer); clearInterval(healthTimer) })
</script>

<template>
  <div class="cards">
    <div class="stat"><div class="num">{{ summary.total ?? '-' }}</div><div class="label">设备总数</div></div>
    <div class="stat"><div class="num green">{{ summary.online ?? '-' }}</div><div class="label">在线</div></div>
    <div class="stat"><div class="num orange">{{ summary.alarmCount ?? '-' }}</div><div class="label">近 1 小时告警</div></div>
    <div class="stat"><div class="num red">{{ summary.critical ?? '-' }}</div><div class="label">严重告警</div></div>
  </div>
  <div v-if="health" class="card">
    <h3>系统健康（10 秒轮询）</h3>
    <div class="health-row">
      <span class="health-item">
        <i class="dot" :class="up(health.mongo) ? 'up' : 'down'"></i>MongoDB
      </span>
      <span class="health-item">
        <i class="dot" :class="up(health.es) ? 'up' : 'down'"></i>Elasticsearch（{{ health.es }}）
      </span>
      <span class="health-item">
        <i class="dot" :class="up(health.hdfs) ? 'up' : 'down'"></i>HDFS
      </span>
      <span class="health-item">
        <i class="dot" :class="!health.kafkaLag.some(g => g.lag < 0) ? 'up' : 'down'"></i>Kafka
      </span>
      <span v-for="g in health.kafkaLag" :key="g.group" class="health-item lag">
        LAG {{ g.group }} = <b>{{ g.lag >= 0 ? g.lag : '—' }}</b>
      </span>
    </div>
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
.health-row { display: flex; gap: 18px; flex-wrap: wrap; align-items: center; font-size: 13px; color: #33414e; }
.health-item { display: inline-flex; align-items: center; gap: 6px; }
.health-item.lag { color: #7b8a99; }
.dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.dot.up { background: #1a8a4a; }
.dot.down { background: #c0392b; }
</style>
