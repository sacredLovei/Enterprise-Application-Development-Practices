<script setup>
import { ref, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  modelValue: { type: Object, default: null }   // { lng, lat } | null
})
const emit = defineEmits(['update:modelValue'])

const mapEl = ref(null)
let map, marker

// 与 DeviceMap 一致的 divIcon：默认 L.marker 图标依赖 leaflet 图片资源，
// Vite 打包后路径失效导致标记不可见（风险 #29）
const PIN_ICON = L.divIcon({
  className: '',
  iconSize: [30, 30],
  html: '<div style="color:#c0392b;width:30px;height:30px;line-height:30px;text-align:center;font-size:26px;text-shadow:0 1px 3px rgba(0,0,0,.5)">📍</div>'
})

onMounted(() => {
  map = L.map(mapEl.value).setView([39.9092, 116.3974], 15)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap'
  }).addTo(map)
  map.on('click', e => {
    const lng = Math.round(e.latlng.lng * 1000000) / 1000000
    const lat = Math.round(e.latlng.lat * 1000000) / 1000000
    setMarker(lng, lat)
    emit('update:modelValue', { lng, lat })
  })
  if (props.modelValue) setMarker(props.modelValue.lng, props.modelValue.lat)
})

watch(() => props.modelValue, v => {
  if (v && map) setMarker(v.lng, v.lat)
})

function setMarker(lng, lat) {
  if (marker) map.removeLayer(marker)
  marker = L.marker([lat, lng], { icon: PIN_ICON }).addTo(map)
}
</script>

<template>
  <div>
    <div ref="mapEl" class="picker-map"></div>
    <div class="hint">
      {{ modelValue ? `已选目标：lng=${modelValue.lng}, lat=${modelValue.lat}` : '点击地图选取目标位置' }}
    </div>
  </div>
</template>

<style scoped>
.picker-map { height: 240px; width: 100%; border-radius: 10px; z-index: 1; }
</style>
