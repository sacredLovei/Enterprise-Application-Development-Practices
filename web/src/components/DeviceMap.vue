<script setup>
import { ref, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  devices: { type: Array, default: () => [] },
  alarms: { type: Array, default: () => [] },
  tasks: { type: Array, default: () => [] }   // 含 targetLng/targetLat 的任务（展示任务目标点）
})

const mapEl = ref(null)
let map, deviceLayer, alarmLayer, targetLayer

const UAV_ICON = L.divIcon({ className: '', iconSize: [34, 34], html: '<div style="background:#2f6fed;color:#fff;border-radius:50%;width:34px;height:34px;line-height:34px;text-align:center;font-size:20px;box-shadow:0 1px 4px rgba(0,0,0,.35)">✈</div>' })
const DOG_ICON = L.divIcon({ className: '', iconSize: [34, 34], html: '<div style="background:#1a8a4a;color:#fff;border-radius:50%;width:34px;height:34px;line-height:34px;text-align:center;font-size:20px;box-shadow:0 1px 4px rgba(0,0,0,.35)">🐕</div>' })
// 告警图标按等级区分：CRITICAL 红色大圆，WARN 橙色（等级口径见设计报告 4.5.1）
const ALARM_CRITICAL_ICON = L.divIcon({ className: '', iconSize: [28, 28], html: '<div style="background:#c0392b;color:#fff;border-radius:50%;width:28px;height:28px;line-height:28px;text-align:center;font-size:17px;font-weight:700;box-shadow:0 0 0 3px rgba(192,57,43,.25),0 1px 4px rgba(0,0,0,.4)">!</div>' })
const ALARM_WARN_ICON = L.divIcon({ className: '', iconSize: [24, 24], html: '<div style="background:#e67e22;color:#fff;border-radius:50%;width:24px;height:24px;line-height:24px;text-align:center;font-size:15px;font-weight:700;box-shadow:0 0 0 3px rgba(230,126,34,.2),0 1px 4px rgba(0,0,0,.35)">!</div>' })
// 任务目标点（定点复核/区域覆盖中心）：红色十字靶标
const TARGET_ICON = L.divIcon({ className: '', iconSize: [26, 26], html: '<div style="color:#c0392b;width:26px;height:26px;text-align:center;line-height:26px;font-size:22px;text-shadow:0 1px 3px rgba(0,0,0,.5)">🎯</div>' })

onMounted(() => {
  map = L.map(mapEl.value).setView([39.9092, 116.3974], 16)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap'
  }).addTo(map)
  deviceLayer = L.layerGroup().addTo(map)
  alarmLayer = L.layerGroup().addTo(map)
  targetLayer = L.layerGroup().addTo(map)
  render()
})

watch(() => [props.devices, props.alarms, props.tasks], render, { deep: true })

function render() {
  if (!map) return
  deviceLayer.clearLayers()
  alarmLayer.clearLayers()
  targetLayer.clearLayers()

  props.devices?.forEach(d => {
    // 设备台账无 location 字段时，用最近遥测位置（接口侧已拼接 lastLocation）
    const loc = d.lastLocation || (d.lng != null ? { lng: d.lng, lat: d.lat } : null)
    if (!loc) return
    const icon = d.deviceType === 'UAV' ? UAV_ICON : DOG_ICON
    L.marker([loc.lat, loc.lng], { icon })
      .bindTooltip(d.deviceId, { permanent: true, direction: 'right', offset: [8, 0], className: 'device-label' })
      .bindPopup(`<b>${d.deviceId}</b><br/>类型：${d.deviceType}<br/>状态：${d.status}<br/>电量：${d.battery}%`)
      .addTo(deviceLayer)
  })

  props.alarms?.forEach(a => {
    const loc = Array.isArray(a.location) ? { lng: a.location[0], lat: a.location[1] }
      : (a.lng != null ? { lng: a.lng, lat: a.lat } : null)
    if (!loc || (loc.lng === 0 && loc.lat === 0)) return
    const icon = a.level === 'CRITICAL' ? ALARM_CRITICAL_ICON : ALARM_WARN_ICON
    L.marker([loc.lat, loc.lng], { icon })
      .bindPopup(`<b>${a.alarmType}</b><br/>等级：${a.level}<br/>${a.description ?? ''}`)
      .addTo(alarmLayer)
  })

  // 任务目标点：仅展示执行中/已下发且带坐标的任务（🎯）
  props.tasks?.forEach(t => {
    if (!t.targetLng || !t.targetLat) return
    if (!['DISPATCHED', 'RUNNING'].includes(t.status)) return
    L.marker([t.targetLat, t.targetLng], { icon: TARGET_ICON })
      .bindTooltip(t.taskId, { permanent: false, direction: 'top', className: 'device-label' })
      .bindPopup(`<b>任务目标点</b><br/>任务：${t.taskId}<br/>类型：${t.taskType}<br/>设备：${t.deviceId}`)
      .addTo(targetLayer)
  })
}
</script>

<template>
  <div ref="mapEl" class="map"></div>
</template>

<style scoped>
.map { height: 520px; width: 100%; border-radius: 10px; z-index: 1; }
/* 常驻设备标签：白底圆角，随图标移动 */
:deep(.device-label) {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #c9d4df;
  border-radius: 6px;
  padding: 1px 6px;
  font-size: 12px;
  font-weight: 600;
  color: #24303c;
  box-shadow: 0 1px 3px rgba(20, 33, 46, 0.2);
}
:deep(.device-label::before) { display: none; }
</style>
