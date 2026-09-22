<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { attachThemedBasemap } from '../utils/basemap'
import { buildCampusLayer, campusContains, CAMPUS_CENTER, CAMPUS_ZOOM } from '../utils/campus'

const props = defineProps({
  modelValue: { type: Object, default: null },   // { lng, lat } | null
  height: { type: String, default: '240px' }     // S99：支持外部传高度（下发任务大地图）
})
const emit = defineEmits(['update:modelValue'])

const mapEl = ref(null)
const outOfCampus = ref(false)   // S104：点击越界提示
let map, marker, detachBasemap

// S98 修补：选点标记与 DeviceMap 任务目标点同款——红色十字靶标 SVG（替换 📍 emoji），
// 外圈脉冲提示"已选中"；颜色经容器 color + currentColor 适配主题（SVG 属性不支持 var()）
const PIN_ICON = L.divIcon({
  className: '',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
  html: '<div class="picker-pin" style="color:var(--danger)"><svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><circle cx="12" cy="12" r="6.5"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/></svg></div>'
})

onMounted(() => {
  // S104：视野对准扩容后的工业园区，园区边界/基地/车间可视化（选点页可见边界）
  map = L.map(mapEl.value).setView(CAMPUS_CENTER, CAMPUS_ZOOM)
  detachBasemap = attachThemedBasemap(map)
  map.addLayer(buildCampusLayer(L))
  map.on('click', e => {
    const lng = Math.round(e.latlng.lng * 1000000) / 1000000
    const lat = Math.round(e.latlng.lat * 1000000) / 1000000
    // S104（用户需求）：派任务的地点限定在园区内，不可外派——越界点击忽略并提示
    if (!campusContains(lng, lat)) {
      outOfCampus.value = true
      return
    }
    outOfCampus.value = false
    setMarker(lng, lat)
    emit('update:modelValue', { lng, lat })
  })
  if (props.modelValue) setMarker(props.modelValue.lng, props.modelValue.lat)
})

onUnmounted(() => {
  if (detachBasemap) detachBasemap()
})

watch(() => props.modelValue, v => {
  if (v && map) setMarker(v.lng, v.lat)
})

function setMarker(lng, lat) {
  if (marker) map.removeLayer(marker)
  marker = L.marker([lat, lng], { icon: PIN_ICON }).addTo(map)
  marker.bindTooltip(`${lng}, ${lat}`, { permanent: false, direction: 'top', className: 'device-label' })
}
</script>

<template>
  <div>
    <div ref="mapEl" class="picker-map" :style="{ height }"></div>
    <div class="hint">
      <template v-if="outOfCampus">
        <span style="color:var(--danger);font-weight:600">✗ 目标位置必须在园区范围内（虚线边界内），不可外派</span>
      </template>
      <template v-else-if="modelValue">
        已选目标：lng={{ modelValue.lng }}, lat={{ modelValue.lat }}
      </template>
      <template v-else>点击园区内选取目标位置（蓝虚线为园区边界）</template>
    </div>
  </div>
</template>

<style scoped>
.picker-map {
  width: 100%;
  border-radius: var(--radius-2);
  z-index: 1;
  cursor: crosshair;   /* 选点交互暗示 */
}

/* 选中靶标：脉冲扩散圈（与总览告警脉冲同族动画） */
:deep(.picker-pin) {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pin-pulse 2s ease-out infinite;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.4));
}
@keyframes pin-pulse {
  0%   { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.45); border-radius: 50%; }
  70%  { box-shadow: 0 0 0 16px rgba(220, 38, 38, 0); border-radius: 50%; }
  100% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0); border-radius: 50%; }
}

/* 标签与控件随主题适配（与 DeviceMap 同一套令牌） */
:deep(.device-label) {
  background: var(--bg-card);
  border: 1px solid var(--border-strong);
  border-radius: 6px;
  padding: 1px 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-1);
  box-shadow: var(--shadow-1);
}
:deep(.device-label::before) { display: none; }
:deep(.leaflet-bar a) {
  background: var(--bg-card);
  color: var(--text-2);
  border-bottom-color: var(--border);
}
:deep(.leaflet-control-attribution) {
  background: var(--bg-sidebar);
  color: var(--text-3);
}
:deep(.leaflet-control-attribution a) { color: var(--text-2); }

/* S104：园区标注（基地/加工车间永久标签） */
:deep(.campus-label) {
  background: var(--bg-card);
  border: 1px solid var(--border-strong);
  border-radius: 6px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-2);
  box-shadow: var(--shadow-1);
}
:deep(.campus-label::before) { display: none; }
</style>
