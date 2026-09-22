// S104：园区单源定义（与后端 CampusBounds / 仿真器 TelemetryGenerator 巡逻环线坐标一致）
// 大型工业园区：lng 116.3952~116.4000 × lat 39.9076~39.9110（约 425m × 380m）

// Leaflet 顺序 [lat, lng]
export const CAMPUS_CENTER = [39.9093, 116.3976]
export const CAMPUS_ZOOM = 15

/** 园区边界多边形顶点（顺时针，Leaflet [lat, lng] 顺序）。 */
export const CAMPUS_POLYGON = [
  [39.9076, 116.3952],
  [39.9076, 116.4000],
  [39.9110, 116.4000],
  [39.9110, 116.3952]
]

/** 基地（设备返航目的地）。 */
export const HOME = { lat: 39.9082, lng: 116.3956 }

/** 加工车间（烟火告警高发点位）。 */
export const WORKSHOP = { lat: 39.9095, lng: 116.3977 }

/** 坐标是否在园区范围内（与后端 CampusBounds.contains 同口径）。 */
export function campusContains(lng, lat) {
  return lng >= 116.3952 && lng <= 116.4000 && lat >= 39.9076 && lat <= 39.9110
}

/**
 * 园区可视化图层：边界虚线多边形 + 淡填充 + 基地/加工车间标记。
 * 供总览地图与派单选点图共用（选点页同样能看到园区边界）。
 */
export function buildCampusLayer(L) {
  const group = L.layerGroup()
  const boundary = L.polygon(CAMPUS_POLYGON, {
    color: '#3b82f6',
    weight: 2,
    dashArray: '8 6',
    fillColor: '#3b82f6',
    fillOpacity: 0.05,
    interactive: false
  })
  group.addLayer(boundary)
  group.addLayer(L.marker([HOME.lat, HOME.lng], {
    icon: L.divIcon({ className: '', html: '<div style="font-size:18px;line-height:1">🏠</div>',
      iconSize: [18, 18], iconAnchor: [9, 9] }),
    interactive: false
  })).addLayer(L.tooltip({ permanent: true, direction: 'right', className: 'campus-label' })
    .setContent('基地').setLatLng([HOME.lat, HOME.lng]))
  group.addLayer(L.marker([WORKSHOP.lat, WORKSHOP.lng], {
    icon: L.divIcon({ className: '', html: '<div style="font-size:18px;line-height:1">🏭</div>',
      iconSize: [18, 18], iconAnchor: [9, 9] }),
    interactive: false
  }))
  group.addLayer(L.tooltip({ permanent: true, direction: 'right', className: 'campus-label' })
    .setContent('加工车间').setLatLng([WORKSHOP.lat, WORKSHOP.lng]))
  return group
}
