<script setup>
// S99-b：告警详情子页（子项）——信息条 + 证据照片满幅 + 复核面板
// 业务逻辑与原 Alarms.vue 详情面板一致（S62 证据链 / S68 复核 / S75 照片 / S83 复判 / S84 锁定 / S96 带令牌取图）
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../api/request'
import { formatTime } from '../utils/time'
import { useAuthImage } from '../utils/authImage'

const route = useRoute()
const router = useRouter()

const detail = ref(null)
const reviewNote = ref('')   // S68：人工复核备注
const reviewing = ref(false) // S74：复核提交中
const reviewMsg = ref('')    // S74：复核结果提示
const photoFile = ref(null)  // S75：现场照片
const bigImg = ref('')       // S81：大图预览

// S96：/api/files/** 需 Authorization 头，经 axios blob 带令牌取图
const snapPath = computed(() => (detail.value ? '/files/' + detail.value.alarmId : ''))
const reviewPath = computed(() => (detail.value && detail.value.review && detail.value.review.imagePath
  ? '/files/' + detail.value.alarmId + '/review' : ''))
const photoPath = computed(() => (detail.value && detail.value.review && detail.value.review.manualPhotoPath
  ? '/files/' + detail.value.alarmId + '/review-photo' : ''))
const snapSrc = useAuthImage(snapPath)
const reviewSrc = useAuthImage(reviewPath)
const photoSrc = useAuthImage(photoPath)

function statusText(s) {
  return { PENDING: '待复核', CONFIRMED: '确认属实', FALSE_ALARM: '误报', RESOLVED: '已处置' }[s] || s
}

// S64 逻辑修复：证据图来源标签按告警类型+设备类型动态生成
function sourceLabel(a) {
  if (a.alarmType === 'DEVICE_OFFLINE' || a.alarmType === 'BATTERY_LOW') return '事件记录图（设备状态）'
  if (a.deviceType === 'UAV') return '高空发现原图（无人机）'
  if (a.deviceType === 'ROBOT_DOG') return '设备自报事件图（机器狗）'
  return '告警事件记录图'
}

// S84：复核入口状态锁——PENDING 可初判；机器狗已初判可复判；人工已判锁定；RESOLVED 关闭
function reviewActionsVisible() {
  if (!detail.value) return false
  if (detail.value.status === 'RESOLVED') return false
  if (detail.value.status === 'PENDING') return true
  return detail.value.review && detail.value.review.reviewerDeviceId !== 'MANUAL'
}

function reviewable(a) {
  return a.alarmType !== 'DEVICE_OFFLINE' && a.alarmType !== 'BATTERY_LOW'
}

async function load() {
  try {
    detail.value = await request.get('/alarms/' + route.params.alarmId)
  } catch (e) {
    console.error('alarm detail failed', e)
  }
}

function onPhotoPick(e) {
  photoFile.value = e.target.files[0] || null
}

// S68 人工复核（IT009）：S75 支持现场照片一并提交
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
    reviewMsg.value = '✓ 复核成功：已标记为' + (conclusion === 'CONFIRMED' ? '确认属实' : '误报')
    setTimeout(() => { reviewMsg.value = '' }, 3000)
  } catch (e) {
    reviewMsg.value = '✗ 复核失败：' + (e.response?.data?.message || '服务异常，请稍后重试')
    setTimeout(() => { reviewMsg.value = '' }, 4000)
  } finally {
    reviewing.value = false
  }
}

onMounted(load)

// S81：点击证据图放大
function zoomImg(src) {
  bigImg.value = src
}
</script>

<template>
  <!-- S102 修复：单根节点（页面过渡要求） -->
  <div class="page-root">
  <div v-if="detail">
    <!-- 信息条：标题 + 元数据 -->
    <div class="card head-card">
      <div class="head-row">
        <div>
          <h3 class="alarm-title">{{ detail.alarmId }}</h3>
          <div class="meta">
            <span class="badge" :class="detail.level === 'CRITICAL' ? 'critical' : 'warn'">{{ detail.level }}</span>
            <span class="meta-item">{{ detail.alarmType }}</span>
            <span class="meta-item">设备 {{ detail.deviceId }}</span>
            <span class="meta-item">{{ formatTime(detail.occurredTime) }}</span>
            <span class="badge" :class="{ ok: detail.status === 'CONFIRMED', warn: detail.status === 'FALSE_ALARM' }">{{ statusText(detail.status) }}</span>
          </div>
          <p class="desc-text">{{ detail.description }}</p>
        </div>
        <button class="ghost small back-btn" @click="router.push('/alarms/list')">← 返回列表</button>
      </div>
    </div>

    <!-- 证据照片：满幅两列大图 -->
    <div class="evidence-grid">
      <div class="card evidence">
        <div class="evidence-title">{{ sourceLabel(detail) }}</div>
        <img v-if="snapSrc" :src="snapSrc" :alt="sourceLabel(detail)" class="evidence-img" @click="zoomImg(snapSrc)" />
        <div v-else class="hint" style="padding: 60px 0; text-align: center">证据图加载中…</div>
      </div>
      <div class="card evidence">
        <div class="evidence-title">复核结果</div>
        <template v-if="detail.review">
          <img v-if="reviewSrc" :src="reviewSrc" alt="红外复核图" class="evidence-img" @click="zoomImg(reviewSrc)" />
          <div v-if="!reviewSrc && detail.review.manualPhotoPath" class="hint" style="padding:20px 0 0">现场照片加载中…</div>
          <div v-if="detail.review.autoConclusion" class="hint" style="margin-top:8px">
            机器狗初判：
            <span class="badge" :class="{ ok: detail.review.autoConclusion === 'CONFIRMED', warn: detail.review.autoConclusion === 'FALSE_ALARM' }">
              {{ statusText(detail.review.autoConclusion) }}
            </span>
            （{{ detail.review.autoReviewerDeviceId }}<span v-if="detail.review.autoReviewedAt"> · {{ formatTime(detail.review.autoReviewedAt) }}</span>）
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
          <div v-if="photoSrc" style="margin-top:10px">
            <div class="evidence-title" style="font-size:12px">现场照片（人工拍摄）</div>
            <img :src="photoSrc" alt="现场照片" class="evidence-img small-img" @click="zoomImg(photoSrc)" />
          </div>
        </template>
        <div v-else class="hint" style="padding: 60px 0; text-align: center">
          <template v-if="reviewable(detail)">尚未复核（自动派单进行中）</template>
          <template v-else>设备状态类告警不派现场复核（由人工处置）</template>
        </div>
      </div>
    </div>

    <!-- S68 人工复核（IT009）；S83 复判；S84 状态锁 -->
    <div v-if="reviewActionsVisible()" class="card">
      <div class="review-actions">
        <span class="review-actions-title">
          {{ detail.review && detail.review.reviewerDeviceId !== 'MANUAL' ? '人工复判（推翻机器狗结论，人工为终审）：' : '人工复核：' }}
        </span>
        <input v-model="reviewNote" placeholder="复核备注（可选）" style="flex:1;min-width:200px" />
        <label class="ghost small photo-btn">现场照片
          <input id="review-photo-input" type="file" accept="image/*" style="display:none" @change="onPhotoPick" />
        </label>
        <span v-if="photoFile" class="hint" style="margin:0">{{ photoFile.name }}</span>
        <button :disabled="reviewing" @click="submitReview('CONFIRMED')">确认属实</button>
        <button class="ghost" :disabled="reviewing" @click="submitReview('FALSE_ALARM')">误报</button>
        <span v-if="reviewMsg" class="review-toast">{{ reviewMsg }}</span>
      </div>
    </div>
    <div v-else-if="detail.status !== 'RESOLVED' && detail.review && detail.review.reviewerDeviceId === 'MANUAL'" class="card">
      <div class="hint" style="margin:0">🔒 人工终审已完成，结论锁定（{{ statusText(detail.status) }}）——如需更改请联系管理员重新打开</div>
    </div>
  </div>
  <div v-else class="card"><div class="hint" style="padding:40px 0;text-align:center">加载中…</div></div>

  <!-- S81 大图预览遮罩 -->
  <div v-if="bigImg" class="lightbox" @click="bigImg = null">
    <img :src="bigImg" class="lightbox-img" alt="大图预览" />
  </div>
  </div>
</template>

<style scoped>
.head-card { margin-bottom: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.alarm-title { font-family: var(--font-mono); font-size: 18px; margin-bottom: 8px; }
.meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; font-size: 13px; color: var(--text-2); }
.meta-item { color: var(--text-2); }
.desc-text { margin-top: 10px; font-size: 14px; color: var(--text-1); line-height: 1.6; }
.back-btn { flex-shrink: 0; }

.evidence-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); gap: 16px; margin-bottom: 16px; }
.evidence { margin-bottom: 0; }
.evidence-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; color: var(--text-1); }
.evidence-img { width: 100%; border: 1px solid var(--border); border-radius: var(--radius-1); display: block; cursor: zoom-in; }
.small-img { max-width: 420px; }
.review-conclusion { margin-top: 8px; font-size: 13px; color: var(--text-1); }
.review-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.review-actions-title { font-size: 13px; color: var(--text-1); font-weight: 600; }
.review-toast { font-size: 13px; font-weight: 600; color: var(--ok); }
.photo-btn { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }
.lightbox { position: fixed; inset: 0; background: rgba(10, 14, 20, 0.88); display: flex; align-items: center; justify-content: center; z-index: 9999; cursor: zoom-out; }
.lightbox-img { width: 94vw; height: 94vh; object-fit: contain; border-radius: var(--radius-1); box-shadow: var(--shadow-2); }
</style>
