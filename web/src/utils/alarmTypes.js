// S103：告警类型/分类的统一展示口径（后端 AlarmCategories 的前端镜像）
// SECURITY 安防类（默认显示）；DEVICE 设备运维类（默认隐藏，查询可选）

export const ALARM_TYPES = {
  PERIMETER_BREACH: '周界入侵',
  FIRE_SMOKE: '烟火告警',
  DEVICE_OVERHEAT: '设备过热',
  BATTERY_LOW: '电量不足',
  DEVICE_OFFLINE: '设备离线'
}

export const SECURITY_TYPES = ['PERIMETER_BREACH', 'FIRE_SMOKE']
export const DEVICE_TYPES = ['DEVICE_OVERHEAT', 'BATTERY_LOW', 'DEVICE_OFFLINE']

export function typeLabel(t) {
  return ALARM_TYPES[t] || t
}

export function categoryOf(alarmType) {
  return SECURITY_TYPES.includes(alarmType) ? 'SECURITY' : 'DEVICE'
}
