// S98 修补：共享的主题化底图工具——DeviceMap 与 TargetPicker 统一使用，
// 避免两处各自维护导致风格漂移（本次 TargetPicker 漏焕新即此类问题）。
//
// 底图策略（与 S97-c 一致，教训见 STATE 风险 #48/#50）：
// - 亮色 Esri Light Gray / 暗色 Esri Dark Gray（低饱和、免 key、随主题自动切换）；
// - 降级保守化：连续 3 次瓦片失败且从未成功才回退 OSM，一次成功即视为可用；
// - CARTO 免费匿名瓦片已返回"API KEY REQUIRED"水印瓦片（HTTP 200），勿再使用。
import L from 'leaflet'

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

/**
 * 为地图挂载随主题切换的底图。
 * @returns {() => void} 解绑函数（组件卸载时调用）
 */
export function attachThemedBasemap(map) {
  let baseLayer = null
  let tileOk = false        // 当前底图是否有瓦片成功加载
  let tileFails = 0         // 连续失败计数（成功即清零）
  let usingFallback = false // 是否已永久回退 OSM

  function setBaseLayer() {
    const key = usingFallback ? 'fallback'
      : (document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light')
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
        usingFallback = true   // 风险 #48：一次失败不降级，连续 3 次且从未成功才降级
        setBaseLayer()
      }
    })
    baseLayer.addTo(map)
  }

  function onThemeChanged() {
    if (!usingFallback) setBaseLayer()
  }
  window.addEventListener('inspection-theme-changed', onThemeChanged)

  setBaseLayer()
  return () => window.removeEventListener('inspection-theme-changed', onThemeChanged)
}
