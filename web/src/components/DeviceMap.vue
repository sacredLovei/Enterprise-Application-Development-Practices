<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getTheme } from '../utils/theme'

const props = defineProps({
  devices: { type: Array, default: () => [] },
  alarms: { type: Array, default: () => [] },
  tasks: { type: Array, default: () => [] },   // 含 targetLng/targetLat 的任务（展示任务目标点）
  track: { type: Array, default: null }        // S69：设备历史轨迹 [{ts,lng,lat}]，null=无轨迹
})

const emit = defineEmits(['track-requested', 'track-clear'])

const mapEl = ref(null)
let map, deviceLayer, alarmLayer, targetLayer, trackLayer

// S98：设备标记增量管理——deviceId -> {marker, latlng(当前显示位置), iconKey, animId}
// 替代原 clearLayers 重建（重建会让标记每秒销毁重建，无法做移动动画）
let deviceMarkers = new Map()

// ============================================================
// S97-c 地图焕新
// 1) 底图：CARTO Positron（亮）/ Dark Matter（暗）——低饱和、专为数据可视化设计；
//    降级保守化（风险 #48 教训）：连续 3 次瓦片失败且从未成功才回退 OSM，
//    一次成功即视为可用，避免代理抖动导致底图被永久换掉。
// 2) 设备标记：矢量 SVG 图标（无人机三角翼 / 机器狗爪印），颜色=类型+状态；
//    告警点：等级配色 + CRITICAL 脉冲扩散圈。
// 颜色全部走 tokens.css 变量——divIcon 注入 DOM 后随主题自动适配。
// ============================================================

// 底图换源（S97-c 验收实测修正）：CARTO 免费匿名瓦片 2026 起对无 key 请求返回
// "API KEY REQUIRED" 水印瓦片（tileload 仍成功，风险 #48 的"成功即可用"判定无法拦截），
// 改用 Esri Canvas Light/Dark Gray——同为低饱和数据可视化底图，免 key（z/y/x 顺序，原生最高 16 级、上方超采样）。
const BASEMAPS = {
  light: {
    url: 'https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}',
    attribution: 'Tiles &copy; Esri &mdash; Esri, HERE, Garmin, FAO, USGS, NGA',
    maxNativeZoom: 16
  },
  dark: {
    url: 'https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}',
    attribution: 'Tiles &copy; Esri &mdash; Esri, HERE, Garmin, FAO, USGS, NGA',
    maxNativeZoom: 16
  },
  fallback: {
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; OpenStreetMap',
    maxNativeZoom: 19
  }
}

let baseLayer = null
let tileOk = false        // 当前底图是否有瓦片成功加载
let tileFails = 0         // 连续失败计数（成功即清零）
let usingFallback = false // 是否已永久回退 OSM

function currentBaseKey() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

function setBaseLayer() {
  if (!map) return
  const key = usingFallback ? 'fallback' : currentBaseKey()
  const conf = BASEMAPS[key]
  if (baseLayer) map.removeLayer(baseLayer)
  tileOk = false
  tileFails = 0
  baseLayer = L.tileLayer(conf.url, {
    attribution: conf.attribution,
    subdomains: key === 'fallback' ? 'abc' : '',
    maxNativeZoom: conf.maxNativeZoom,
    maxZoom: 19
  })
  baseLayer.on('tileload', () => { tileOk = true; tileFails = 0 })
  baseLayer.on('tileerror', () => {
    tileFails++
    if (tileFails >= 3 && !tileOk && !usingFallback) {
      usingFallback = true        // 风险 #48：连续 3 次失败且从未成功才降级，一次成功即视为可用
      setBaseLayer()
    }
  })
  baseLayer.addTo(map)
}

// 设备标记：divIcon 内嵌 SVG，颜色经 CSS 变量随主题适配
function deviceIcon(deviceType, status) {
  const offline = status !== 'ONLINE'
  const cls = 'dev-pin ' + (deviceType === 'UAV' ? 'pin-uav' : 'pin-dog') + (offline ? ' pin-offline' : '')
  const glyph = deviceType === 'UAV'
    ? '<svg viewBox="0 0 24 24" width="17" height="17"><path d="m2 21 20-9L2 3l3 9-3 9z" fill="#fff"/></svg>'
    : '<svg viewBox="0 0 24 24" width="17" height="17"><g fill="#fff"><circle cx="8" cy="6.5" r="2"/><circle cx="16" cy="6.5" r="2"/><circle cx="4.8" cy="11.5" r="1.8"/><circle cx="19.2" cy="11.5" r="1.8"/><path d="M12 10.5c-3 0-5.5 2.5-5.5 5 0 1.7 1.3 3 3 3 .9 0 1.7-.4 2.5-.4s1.6.4 2.5.4c1.7 0 3-1.3 3-3 0-2.5-2.5-5-5.5-5z"/></g></svg>'
  return L.divIcon({
    className: '',
    iconSize: [34, 34],
    iconAnchor: [17, 17],
    html: `<div class="${cls}">${glyph}</div>`
  })
}

// 告警点：等级配色 + CRITICAL 脉冲圈
function alarmIcon(level) {
  const cls = 'alarm-pin ' + (level === 'CRITICAL' ? 'alarm-critical' : 'alarm-warn')
  return L.divIcon({
    className: '',
    iconSize: [26, 26],
    iconAnchor: [13, 13],
    html: `<div class="${cls}"><span>!</span></div>`
  })
}

// 任务目标点：红色十字靶标（替换 🎯 emoji）——SVG 属性不支持 var()，
// 经容器 color + currentColor 适配主题
const TARGET_ICON = L.divIcon({
  className: '',
  iconSize: [26, 26],
  iconAnchor: [13, 13],
  html: '<div class="target-pin" style="color:var(--danger)"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><circle cx="12" cy="12" r="6.5"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/></svg></div>'
})

function onThemeChanged() {
  if (!usingFallback) setBaseLayer()
  render()   // 标记用 CSS 变量可自动适配，但 tooltip/popup 需重渲染的场景保留刷新
}

onMounted(() => {
  map = L.map(mapEl.value).setView([39.9092, 116.3974], 16)
  setBaseLayer()
  deviceLayer = L.layerGroup().addTo(map)
  alarmLayer = L.layerGroup().addTo(map)
  targetLayer = L.layerGroup().addTo(map)
  trackLayer = L.layerGroup().addTo(map)
  // S69：Leaflet 弹窗 HTML 无法绑定 Vue 事件，经 window 桥接转发轨迹请求
  window.__dshTrack = (id) => emit('track-requested', id)
  window.addEventListener('inspection-theme-changed', onThemeChanged)
  render()
  renderTrack()
})

onUnmounted(() => {
  window.removeEventListener('inspection-theme-changed', onThemeChanged)
})

watch(() => [props.devices, props.alarms, props.tasks], render, { deep: true })
watch(() => props.track, renderTrack)

const TYPE_LABEL = {
  PERIMETER_PATROL: '周界巡逻',
  AREA_COVER: '区域覆盖',
  POINT_REVIEW: '定点复核',
  RETURN_HOME: '返航'
}

/** 该设备当前活跃任务（执行中优先，其次排队），供悬浮面板展示。 */
function activeTasks(deviceId) {
  const list = (props.tasks || []).filter(t =>
    t.deviceId === deviceId && ['DISPATCHED', 'RUNNING'].includes(t.status))
  return list.sort((a, b) => (a.status === 'RUNNING' ? 0 : 1) - (b.status === 'RUNNING' ? 0 : 1))
}

/** 悬浮面板 HTML（信息 + 当前任务与备注，用户要求）。 */
function popupHtml(d) {
  const acts = activeTasks(d.deviceId)
  let taskHtml = ''
  if (acts.length > 0) {
    const cur = acts[0]
    const label = TYPE_LABEL[cur.taskType] || cur.taskType
    taskHtml = `<br/>━━━━━━━━━━<br/><b>任务：${label}</b>` +
      `<br/>状态：${cur.status === 'RUNNING' ? '执行中' : '已下发待执行'}` +
      `<br/>备注：${cur.remark || '—'}` +
      `<br/><span style="font-size:11px;color:var(--text-3)">${cur.taskId}</span>`
    if (acts.length > 1) {
      taskHtml += `<br/>另有 ${acts.length - 1} 个任务排队中`
    }
  }
  return `<b>${d.deviceId}</b><br/>类型：${d.deviceType}<br/>状态：${d.status}<br/>电量：${d.battery}%${taskHtml}` +
    `<br/><a href="#" onclick="window.__dshTrack('${d.deviceId}');return false;" style="font-size:12px">📈 最近 10 分钟轨迹</a>`
}

/** S98：标记平滑移动——rAF 缓动插值（~900ms 走完一段），远距跳变（初始/轨迹回放后回位）直接落点。 */
function moveSmoothly(entry, lat, lng) {
  const target = L.latLng(lat, lng)
  if (entry.latlng.equals(target)) return
  const from = entry.latlng.clone()
  if (from.distanceTo(target) > 150) {   // 大位移视为异常跳变，不动画
    entry.latlng = target
    entry.marker.setLatLng(target)
    return
  }
  cancelAnimationFrame(entry.animId)
  const duration = 900
  const t0 = performance.now()
  const ease = t => 1 - Math.pow(1 - t, 3)
  const step = now => {
    const p = Math.min(1, (now - t0) / duration)
    const k = ease(p)
    const cur = L.latLng(from.lat + (target.lat - from.lat) * k, from.lng + (target.lng - from.lng) * k)
    entry.latlng = cur
    entry.marker.setLatLng(cur)
    if (p < 1) entry.animId = requestAnimationFrame(step)
  }
  entry.animId = requestAnimationFrame(step)
}

function render() {
  if (!map) return
  alarmLayer.clearLayers()
  targetLayer.clearLayers()

  // 设备标记：增量更新（S98）——位置插值移动、状态变 更换图标/面板，消失才移除
  const seen = new Set()
  props.devices?.forEach(d => {
    // 设备台账无 location 字段时，用最近遥测位置（接口侧已拼接 lastLocation）
    const loc = d.lastLocation || (d.lng != null ? { lng: d.lng, lat: d.lat } : null)
    if (!loc) return
    seen.add(d.deviceId)
    const icon = deviceIcon(d.deviceType, d.status)
    const iconKey = d.deviceType + '/' + d.status
    let entry = deviceMarkers.get(d.deviceId)
    if (!entry) {
      const marker = L.marker([loc.lat, loc.lng], { icon })
        .bindTooltip(d.deviceId, { permanent: true, direction: 'right', offset: [10, 0], className: 'device-label' })
        .bindPopup(popupHtml(d))
        .addTo(deviceLayer)
      deviceMarkers.set(d.deviceId, { marker, latlng: L.latLng(loc.lat, loc.lng), iconKey, animId: 0 })
    } else {
      if (entry.iconKey !== iconKey) {
        entry.marker.setIcon(icon)
        entry.iconKey = iconKey
      }
      entry.marker.setPopupContent(popupHtml(d))
      moveSmoothly(entry, loc.lat, loc.lng)
    }
  })
  for (const [id, entry] of deviceMarkers) {
    if (!seen.has(id)) {
      cancelAnimationFrame(entry.animId)
      deviceLayer.removeLayer(entry.marker)
      deviceMarkers.delete(id)
    }
  }

  props.alarms?.forEach(a => {
    const loc = Array.isArray(a.location) ? { lng: a.location[0], lat: a.location[1] }
      : (a.lng != null ? { lng: a.lng, lat: a.lat } : null)
    if (!loc || (loc.lng === 0 && loc.lat === 0)) return
    const icon = alarmIcon(a.level)
    L.marker([loc.lat, loc.lng], { icon })
      .bindPopup(`<b>${a.alarmType}</b><br/>等级：${a.level}<br/>${a.description ?? ''}`)
      .addTo(alarmLayer)
  })

  // 任务目标点：仅展示执行中/已下发且带坐标的任务（十字靶标）
  props.tasks?.forEach(t => {
    if (!t.targetLng || !t.targetLat) return
    if (!['DISPATCHED', 'RUNNING'].includes(t.status)) return
    L.marker([t.targetLat, t.targetLng], { icon: TARGET_ICON })
      .bindTooltip(t.taskId, { permanent: false, direction: 'top', className: 'device-label' })
      .bindPopup(`<b>任务目标点</b><br/>任务：${t.taskId}<br/>类型：${t.taskType}<br/>设备：${t.deviceId}`)
      .addTo(targetLayer)
  })
}

/** S69：轨迹回放图层——渐隐折线（旧点淡、新点浓）+ 起终点标记。 */
function renderTrack() {
  if (!map || !trackLayer) return
  trackLayer.clearLayers()
  const pts = props.track
  if (!pts || pts.length < 2) return
  const latlngs = pts.map(p => [p.lat, p.lng])
  // 渐隐：分段绘制，透明度随进度 0.25 → 0.9（S97-c，替代单色折线）
  const segs = latlngs.length - 1
  for (let i = 0; i < segs; i++) {
    const op = 0.25 + 0.65 * (i / segs)
    L.polyline([latlngs[i], latlngs[i + 1]], {
      color: getComputedStyle(document.documentElement).getPropertyValue('--accent').trim() || '#2563eb',
      weight: 3,
      opacity: op
    }).addTo(trackLayer)
  }
  L.circleMarker(latlngs[0], { radius: 5, color: 'var(--accent)', fillColor: 'var(--accent)', fillOpacity: 1 })
    .bindTooltip('起点', { direction: 'top', className: 'device-label' }).addTo(trackLayer)
  L.circleMarker(latlngs[latlngs.length - 1], { radius: 5, color: 'var(--ok)', fillColor: 'var(--ok)', fillOpacity: 1 })
    .bindTooltip('当前位置', { direction: 'top', className: 'device-label' }).addTo(trackLayer)
  map.fitBounds(L.latLngBounds(latlngs), { padding: [30, 30] })
}
</script>

<template>
  <div ref="mapEl" class="map"></div>
</template>

<style scoped>
.map { height: 520px; width: 100%; border-radius: var(--radius-2); z-index: 1; }

/* 常驻设备标签：卡片化，随主题适配 */
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

/* S97-c：设备矢量标记（divIcon 注入节点在组件子树内，经 :deep 命中） */
:deep(.dev-pin) {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--bg-card);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
}
:deep(.pin-uav) { background: var(--accent); }
:deep(.pin-dog) { background: var(--ok); }
:deep(.pin-offline) { background: var(--text-3); }
:deep(.pin-offline svg) { opacity: 0.85; }

/* 告警点：实心圆 + CRITICAL 脉冲扩散圈 */
:deep(.alarm-pin) {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  border: 2px solid var(--bg-card);
}
:deep(.alarm-critical) { background: var(--danger); animation: alarm-pulse 2s ease-out infinite; }
:deep(.alarm-warn) { background: var(--warn); }
:deep(.target-pin) {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.4));
}
@keyframes alarm-pulse {
  0%   { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.55); }
  70%  { box-shadow: 0 0 0 14px rgba(220, 38, 38, 0); }
  100% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0); }
}

/* Leaflet 控件与弹窗随主题适配（全部走变量，暗色由 tokens 自动生效） */
:deep(.leaflet-popup-content-wrapper),
:deep(.leaflet-popup-tip) { background: var(--bg-card); color: var(--text-1); }
:deep(.leaflet-popup-content) { font-size: 13px; line-height: 1.55; }
:deep(.leaflet-popup-content a) { color: var(--accent); }
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
</style>
