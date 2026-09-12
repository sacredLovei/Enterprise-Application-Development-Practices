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
  marker = L.marker([lat, lng]).addTo(map)
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
