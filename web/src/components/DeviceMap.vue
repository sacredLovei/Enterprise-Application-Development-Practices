<script setup>
import { ref, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  devices: { type: Array, default: () => [] },
  alarms: { type: Array, default: () => [] }
})

const mapEl = ref(null)
let map, deviceLayer, alarmLayer

const UAV_ICON = L.divIcon({ className: '', html: '<div style="background:#2f6fed;color:#fff;border-radius:50%;width:18px;height:18px;line-height:18px;text-align:center;font-size:11px">✈</div>' })
const DOG_ICON = L.divIcon({ className: '', html: '<div style="background:#1a8a4a;color:#fff;border-radius:50%;width:18px;height:18px;line-height:18px;text-align:center;font-size:11px">🐕</div>' })
const ALARM_ICON = L.divIcon({ className: '', html: '<div style="background:#c0392b;color:#fff;border-radius:50%;width:16px;height:16px;line-height:16px;text-align:center;font-size:10px">!</div>' })

onMounted(() => {
  map = L.map(mapEl.value).setView([39.9092, 116.3974], 16)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap'
  }).addTo(map)
  deviceLayer = L.layerGroup().addTo(map)
  alarmLayer = L.layerGroup().addTo(map)
  render()
})

watch(() => [props.devices, props.alarms], render, { deep: true })

function render() {
  if (!map) return
  deviceLayer.clearLayers()
  alarmLayer.clearLayers()

  props.devices?.forEach(d => {
    // 设备台账无 location 字段时，用最近遥测位置（接口侧已拼接 lastLocation）
    const loc = d.lastLocation || (d.lng != null ? { lng: d.lng, lat: d.lat } : null)
    if (!loc) return
    const icon = d.deviceType === 'UAV' ? UAV_ICON : DOG_ICON
    L.marker([loc.lat, loc.lng], { icon })
      .bindPopup(`<b>${d.deviceId}</b><br/>类型：${d.deviceType}<br/>状态：${d.status}<br/>电量：${d.battery}%`)
      .addTo(deviceLayer)
  })

  props.alarms?.forEach(a => {
    const loc = Array.isArray(a.location) ? { lng: a.location[0], lat: a.location[1] }
      : (a.lng != null ? { lng: a.lng, lat: a.lat } : null)
    if (!loc || (loc.lng === 0 && loc.lat === 0)) return
    L.marker([loc.lat, loc.lng], { icon: ALARM_ICON })
      .bindPopup(`<b>${a.alarmType}</b><br/>等级：${a.level}<br/>${a.description ?? ''}`)
      .addTo(alarmLayer)
  })
}
</script>

<template>
  <div ref="mapEl" class="map"></div>
</template>

<style scoped>
.map { height: 520px; width: 100%; border-radius: 10px; z-index: 1; }
</style>
