<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'
import { formatTime } from '../utils/time'

const form = ref({ alarmType: '', level: '', deviceId: '', keyword: '', from: 'now-24h', to: 'now' })
const devices = ref([])
const page = ref(0)
const size = 15
const result = ref({ total: 0, records: [] })
const detail = ref(null)   // S62：告警详情（含复核证据链）
const reviewNote = ref('')   // S68：人工复核备注
const reviewing = ref(false) // S74：复核提交中（按钮禁用防重复点击）
const reviewMsg = ref('')    // S74：复核成功提示（3 秒后消失）
const photoFile = ref(null)  // S75：人工复核现场照片
const bigImg = ref('')       // S81：大图预览

// S81：点击证据图放大
function zoomImg(src) {
  bigImg.value = src
}
let timer, debounceTimer, deviceTimer
let seq = 0   // 请求序号：丢弃过期响应，防止自动刷新与手动检索竞态覆盖新结果

// 设备下拉选项：动态取自设备台账（/api/devices），增删设备自动更新，无硬编码
async function loadDevices() {
  try {
    devices.value = await request.get('/devices')
  } catch (e) {
    console.error('load devices failed', e)
  }
}

async function search() {
  const my = ++seq
  try {
    const data = await request.post('/search/alarms', {
      alarmType: form.value.alarmType || null,
      level: form.value.level || null,
      deviceId: form.value.deviceId || null,
      keyword: form.value.keyword || null,
      from: form.value.from,
      to: form.value.to,
      page: page.value,
      size
    })
    if (my === seq) result.value = data   // 仅应用最新请求的结果
  } catch (e) {
    console.error(e)
  }
}

// S62 证据链详情：Mongo 权威文档（含复核子文档），高空原图 + 机器狗红外复核图并列展示（TC032）
async function openDetail(alarmId) {
  try {
    detail.value = await request.get('/alarms/' + alarmId)
  } catch (e) {
    console.error('alarm detail failed', e)
  }
}

// S68 人工复核（IT009 前端入口）：PENDING 告警可一键复核；S75 支持现场照片（multipart 一并提交）
function onPhotoPick(e) {
  photoFile.value = e.target.files[0] || null
}

async function submitReview(conclusion) {
  if (!detail.value || reviewing.value) return
  reviewing.value = true
  reviewMsg.value = ''
  try {
    const fd = new FormData()
    fd.append('conclusion', conclusion)
    fd.append('note', reviewNote.value || '人工复核（前端）')
    if (photoFile.value) fd.append('file', photoFile.value)
    await request.post('/alarms/' + detail.value.alarmId + '/review-photo', fd)
    reviewNote.value = ''
    photoFile.value = null
    const fileInput = document.getElementById('review-photo-input')
    if (fileInput) fileInput.value = ''
    detail.value = await request.get('/alarms/' + detail.value.alarmId)
    search()   // 同步刷新列表状态徽标
    reviewMsg.value = '✓ 复核成功：已标记为' + (conclusion === 'CONFIRMED' ? '确认属实' : '误报')
    setTimeout(() => { reviewMsg.value = '' }, 3000)   // S74 反馈强化：3 秒后提示消失
  } catch (e) {
    reviewMsg.value = '✗ 复核失败：' + (e.response?.data?.message || '服务异常，请稍后重试')
    setTimeout(() => { reviewMsg.value = '' }, 4000)
  } finally {
    reviewing.value = false
  }
}

function statusText(s) {
  return { PENDING: '待复核', CONFIRMED: '确认属实', FALSE_ALARM: '误报', RESOLVED: '已处置' }[s] || s
}

// S64 逻辑修复（用户反馈）：证据图来源标签按告警类型+设备类型动态生成——
// 设备状态类告警（离线/电量）是平台判定或设备自报，不存在"无人机高空发现"；
// 机器狗自报的事件（过热等）同样不是无人机发现
function sourceLabel(a) {
  if (a.alarmType === 'DEVICE_OFFLINE' || a.alarmType === 'BATTERY_LOW') return '① 事件记录图（设备状态）'
  if (a.deviceType === 'UAV') return '① 高空发现原图（无人机）'
  if (a.deviceType === 'ROBOT_DOG') return '① 设备自报事件图（机器狗）'
  return '① 告警事件记录图'
}

// 设备状态类告警不派现场复核（与 D-21 派单策略一致）
function reviewable(a) {
  return a.alarmType !== 'DEVICE_OFFLINE' && a.alarmType !== 'BATTERY_LOW'
}

// 输入防抖：停止输入 400ms 后自动检索（体验即时，不必每次点按钮）
function onFilterInput() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; search() }, 400)
}

function goPage(p) {
  page.value = Math.max(0, p)
  search()
}

const totalPages = () => {
  // S71 深分页防护：ES 仅可翻前 10,000 条，页码按上限截断
  const maxPages = Math.floor(10000 / size)
  return Math.max(1, Math.min(Math.ceil(result.value.total / size), maxPages))
}

const depthCapped = () => result.value.total > 10000

onMounted(() => {
  loadDevices()
  search()
  timer = setInterval(search, 5000)          // 自动刷新沿用当前筛选与页码
  deviceTimer = setInterval(loadDevices, 30000)   // 设备清单 30s 刷新
})
onUnmounted(() => { clearInterval(timer); clearTimeout(debounceTimer); clearInterval(deviceTimer) })
</script>

<template>
  <div class="card">
    <h3>告警检索（MongoDB 权威 + Elasticsearch 检索副本）</h3>
    <div class="form-row">
      <select v-model="form.alarmType" @change="onFilterInput">
        <option value="">全部类型</option>
        <option value="PERIMETER_BREACH">周界入侵</option>
        <option value="DEVICE_OVERHEAT">设备过热</option>
        <option value="BATTERY_LOW">电量不足</option>
        <option value="DEVICE_OFFLINE">设备离线</option>
      </select>
      <select v-model="form.level" @change="onFilterInput">
        <option value="">全部等级</option>
        <option value="CRITICAL">严重</option>
        <option value="WARN">警告</option>
      </select>
      <select v-model="form.deviceId" @change="onFilterInput">
        <option value="">全部设备</option>
        <option v-for="d in devices" :key="d.deviceId" :value="d.deviceId">
          {{ d.deviceId }}（{{ d.deviceType === 'UAV' ? '无人机' : '机器狗' }}·{{ d.status }}）
        </option>
      </select>
      <input v-model="form.keyword" placeholder="关键词" style="width:140px" @input="onFilterInput" />
      <button @click="page = 0; search()">检索</button>
    </div>
    <div class="hint">
      共 {{ result.total }} 条（时间 {{ form.from }} ~ {{ form.to }}，每 5 秒自动刷新，筛选即输即查）
      <span v-if="depthCapped()" class="warn-hint">⚠ 超过 10,000 条时仅可浏览前 10,000 条，请缩小时间范围或增加筛选条件</span>
    </div>
  </div>

  <div class="card">
    <table class="grid">
      <thead>
        <tr><th>告警编号</th><th>类型</th><th>等级</th><th>设备</th><th>描述</th><th>时间</th><th>状态</th><th>证据链</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in result.records" :key="a.alarmId">
          <td>{{ a.alarmId }}</td>
          <td>{{ a.alarmType }}</td>
          <td><span class="badge" :class="a.level === 'CRITICAL' ? 'critical' : 'warn'">{{ a.level }}</span></td>
          <td>{{ a.deviceId }}</td>
          <td>{{ a.description }}</td>
          <td>{{ formatTime(a.occurredTime) }}</td>
          <td>
            <span class="badge" :class="{ ok: a.status === 'CONFIRMED', warn: a.status === 'FALSE_ALARM' }">{{ statusText(a.status) }}</span>
          </td>
          <td><button class="ghost small" @click="openDetail(a.alarmId)">详情</button></td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <button class="ghost" :disabled="page <= 0" @click="goPage(page - 1)">上一页</button>
      <span>第 {{ page + 1 }} / {{ totalPages() }} 页（每页 {{ size }} 条）</span>
      <button class="ghost" :disabled="page + 1 >= totalPages()" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>

  <!-- S62 证据链详情（TC032）：高空原图 + 机器狗红外复核图并列展示 -->
  <div v-if="detail" class="card">
    <div class="detail-head">
      <h3>告警详情：{{ detail.alarmId }}</h3>
      <button class="ghost small" @click="detail = null">关闭</button>
    </div>
    <div class="detail-meta">
      类型 {{ detail.alarmType }} · 等级 {{ detail.level }} · 设备 {{ detail.deviceId }} ·
      时间 {{ formatTime(detail.occurredTime) }} · 状态
      <span class="badge" :class="{ ok: detail.status === 'CONFIRMED', warn: detail.status === 'FALSE_ALARM' }">{{ statusText(detail.status) }}</span>
    </div>
    <p class="hint">{{ detail.description }}</p>
    <div class="evidence-row">
      <div class="evidence">
        <div class="evidence-title">{{ sourceLabel(detail) }}</div>
        <img :src="'/api/files/' + detail.alarmId" :alt="sourceLabel(detail)" class="evidence-img" @click="zoomImg('/api/files/' + detail.alarmId)" />
      </div>
      <!-- S74 复核结果面板：有复核记录即展示结论+方式+备注；图可选（自动复核红外图/人工现场照片） -->
      <div class="evidence">
        <div class="evidence-title">② 复核结果</div>
        <template v-if="detail.review">
          <img v-if="detail.review.imagePath" :src="'/api/files/' + detail.alarmId + '/review'" alt="红外复核图" class="evidence-img" @click="zoomImg('/api/files/' + detail.alarmId + '/review')" />
          <div v-if="detail.review.autoConclusion" class="hint" style="margin-top:6px">
            机器狗初判：
            <span class="badge" :class="{ ok: detail.review.autoConclusion === 'CONFIRMED', warn: detail.review.autoConclusion === 'FALSE_ALARM' }">
              {{ statusText(detail.review.autoConclusion) }}
            </span>
            （{{ detail.review.autoReviewerDeviceId }}<span v-if="detail.review.autoReviewedAt"> · {{ formatTime(detail.review.autoReviewedAt) }}</span>）
            → 人工终审：
          </div>
          <div class="review-conclusion">
            复核结论：
            <span class="badge" :class="{ ok: detail.review.conclusion === 'CONFIRMED', warn: detail.review.conclusion === 'FALSE_ALARM' }">
              {{ statusText(detail.review.conclusion) }}
            </span>
            · 方式：{{ detail.review.reviewerDeviceId === 'MANUAL' ? '人工复核' : '机器狗自动（' + detail.review.reviewerDeviceId + '）' }}
            <span v-if="detail.review.reviewedAt"> · {{ formatTime(detail.review.reviewedAt) }}</span>
          </div>
          <div class="hint" v-if="detail.review.note">备注：{{ detail.review.note }}</div>
          <div v-if="detail.review.manualPhotoPath" style="margin-top:10px">
            <div class="evidence-title" style="font-size:12px">📷 现场照片（人工拍摄）</div>
            <img :src="'/api/files/' + detail.alarmId + '/review-photo'" alt="现场照片" class="evidence-img" @click="zoomImg('/api/files/' + detail.alarmId + '/review-photo')" />
          </div>
        </template>
        <div v-else class="hint" style="padding: 60px 0; text-align: center">
          <template v-if="reviewable(detail)">尚未复核（自动派单进行中）</template>
          <template v-else>设备状态类告警不派现场复核（由人工处置）</template>
        </div>
      </div>
    </div>
    <!-- S68 人工复核入口（IT009）：待复核告警可一键处置；S74 反馈强化；S75 现场照片；S83 自动结论可复判 -->
    <div v-if="detail.status !== 'RESOLVED'" class="review-actions">
      <span class="review-actions-title">
        {{ detail.review && detail.review.reviewerDeviceId !== 'MANUAL' ? '人工复判（推翻机器狗结论，人工为终审）：' : '人工复核：' }}
      </span>
      <input v-model="reviewNote" placeholder="复核备注（可选）" style="flex:1" />
      <label class="ghost small photo-btn">📷 现场照片
        <input id="review-photo-input" type="file" accept="image/*" style="display:none" @change="onPhotoPick" />
      </label>
      <span v-if="photoFile" class="hint">{{ photoFile.name }}</span>
      <button :disabled="reviewing" @click="submitReview('CONFIRMED')">确认属实</button>
      <button class="ghost" :disabled="reviewing" @click="submitReview('FALSE_ALARM')">误报</button>
      <span v-if="reviewMsg" class="review-toast">{{ reviewMsg }}</span>
    </div>
  </div>
  <!-- S81 大图预览遮罩（点击任意处关闭） -->
  <div v-if="bigImg" class="lightbox" @click="bigImg = null">
    <img :src="bigImg" class="lightbox-img" alt="大图预览" />
  </div>
</template>

<style scoped>
.pager { display: flex; gap: 14px; align-items: center; justify-content: center; margin-top: 12px; font-size: 13px; color: #5c6b7a; }
button:disabled { opacity: .4; cursor: not-allowed; }
.small { font-size: 12px; padding: 3px 10px; }
.ok { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
.detail-head { display: flex; justify-content: space-between; align-items: center; }
.detail-meta { font-size: 13px; color: #5c6b7a; margin-bottom: 4px; }
.evidence-row { display: flex; gap: 20px; flex-wrap: wrap; }
.review-actions { display: flex; gap: 10px; align-items: center; margin-top: 14px; padding-top: 12px; border-top: 1px solid #e2e8ee; }
.review-actions-title { font-size: 13px; color: #33414e; font-weight: 600; }
.review-toast { font-size: 13px; font-weight: 600; color: #1a8a4a; }
.photo-btn { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }
.evidence-img { cursor: zoom-in; }
.lightbox { position: fixed; inset: 0; background: rgba(10, 14, 20, 0.88); display: flex; align-items: center; justify-content: center; z-index: 9999; cursor: zoom-out; }
.lightbox-img { width: 94vw; height: 94vh; object-fit: contain; border-radius: 8px; box-shadow: 0 8px 40px rgba(0, 0, 0, 0.6); }
.evidence { flex: 1 1 320px; }
.evidence-title { font-size: 13px; font-weight: 600; margin-bottom: 6px; color: #33414e; }
.evidence-img { width: 100%; border: 1px solid #e2e8ee; border-radius: 8px; display: block; }
.review-conclusion { margin-top: 8px; font-size: 13px; color: #33414e; }
</style>
