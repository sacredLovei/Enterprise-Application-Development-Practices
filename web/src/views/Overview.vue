<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import DeviceMap from '../components/DeviceMap.vue'
import { connectPositions } from '../utils/live'

const devices = ref([])
const alarms = ref([])
const tasks = ref([])
const summary = ref({})
const track = ref(null)   // S69：设备历史轨迹
const health = ref(null)  // S80：系统健康（Mongo/ES/HDFS/Kafka LAG）
let timer, healthTimer

// ===== S98：实时位置流（SSE 1 秒推送）=====
const liveMap = ref(new Map())   // deviceId -> {lng, lat, status, battery, ts}
const liveState = ref('off')     // off | open | closed | error
let disconnectLive = null

// 叠加层：3s 轮询的 devices 为底，SSE 最新位置覆盖其上（轮询保留作兜底与全量刷新）
const devicesLive = computed(() => devices.value.map(d => {
  const o = liveMap.value.get(d.deviceId)
  return o ? { ...d, lng: o.lng, lat: o.lat, status: o.status, battery: o.battery } : d
}))

function onLiveMsg(msg) {
  const next = new Map(liveMap.value)
  for (const d of msg.devices || []) {
    if (d.lng != null && d.lat != null) {
      next.set(d.deviceId, { lng: d.lng, lat: d.lat, status: d.status, battery: d.battery, ts: msg.ts })
    }
  }
  liveMap.value = next
}

function onLiveState(s) { liveState.value = s }

async function load() {
  try {
    devices.value = await request.get('/devices')
    // S103：主页只显示"重要告警"=安防类（周界入侵/烟火）+ 升级 CRITICAL 的设备离线，
    // 设备常规运维告警（过热/低电/常规离线）不再弹主页
    const r = await request.post('/search/alarms', { from: 'now-1h', to: 'now', category: 'IMPORTANT', page: 0, size: 50 })
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
  timer = setInterval(load, 3000)          // 总览 3s 轮询（S98 起降级为兜底与全量刷新）
  healthTimer = setInterval(loadHealth, 10000)   // 健康面板 10s 轮询
  disconnectLive = connectPositions(onLiveMsg, onLiveState)   // S98 实时位置流
})
onUnmounted(() => {
  clearInterval(timer)
  clearInterval(healthTimer)
  if (disconnectLive) disconnectLive()
})
</script>

<template>
  <!-- S102 修复：页面过渡要求单根节点——多根 fragment 会导致 out-in 过渡卡死白屏 -->
  <div class="page-root">
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
    <h3>园区设备与告警分布（S98：位置实时推送，标记连续移动）</h3>
    <DeviceMap :devices="devicesLive" :alarms="alarms" :tasks="tasks" :track="track"
               @track-requested="showTrack" @track-clear="clearTrack" />
    <div class="hint">
      蓝色标记 = 无人机，绿色 = 机器狗（灰色为离线）；红色 ! 为告警点（严重告警带脉冲圈）；红色靶标为任务目标点。
      底图为 Esri 灰度地图（低饱和，随亮/暗主题自动切换）。
      <span class="live-state" :class="'ls-' + liveState">
        {{ liveState === 'open' ? '● 实时推送中（1 秒级）' : liveState === 'error' ? '● 实时流中断，3 秒轮询兜底' : '● 实时流待连接' }}
      </span>
      点击设备图标 → 「📈 最近 10 分钟轨迹」查看回放；关闭弹窗后轨迹自动清除。
    </div>
  </div>
  </div>
</template>

<style scoped>
.cards { display: flex; gap: 14px; margin-bottom: 16px; }
.stat {
  flex: 1; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-2); padding: 14px;
  text-align: center; box-shadow: var(--shadow-1);
}
.num { font-size: 26px; font-weight: 700; font-family: var(--font-mono); }
.num.green { color: var(--ok); }
.num.orange { color: var(--warn); }
.num.red { color: var(--danger); }
.label { color: var(--text-3); font-size: 12px; margin-top: 4px; }
.health-row { display: flex; gap: 18px; flex-wrap: wrap; align-items: center; font-size: 13px; color: var(--text-2); }
.health-item { display: inline-flex; align-items: center; gap: 6px; }
.health-item.lag { color: var(--text-3); }
.health-item.lag b { color: var(--text-2); font-family: var(--font-mono); }
.dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.dot.up { background: var(--ok); }
.dot.down { background: var(--danger); }
/* S98：实时流状态指示 */
.live-state { margin: 0 8px; font-weight: 600; }
.live-state.ls-open { color: var(--ok); }
.live-state.ls-error, .live-state.ls-closed { color: var(--warn); }
.live-state.ls-off { color: var(--text-3); }
</style>
