<script setup>
// S101：登录页动效升级——粒子背景（ParticleField）+ 指挥雷达面板 + 入场动效。
// 登录逻辑（S92 演示账号 chip 一键填入等）与 S97-d 分栏布局保持不变。
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { saveAuth } from '../api/request'
import ParticleField from '../components/ParticleField.vue'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

// S101 补充：自定义光标动效——光环弹性跟随 + 尾点（仅精确指针且未开启减少动效时启用）
const ring = ref(null)
const dot = ref(null)
let raf = 0
let tx = 0, ty = 0, rx = 0, ry = 0, dxx = 0, dyy = 0

function onMove(e) { tx = e.clientX; ty = e.clientY }

function loop() {
  rx += (tx - rx) * 0.14   // 光环慢半拍（弹性跟随）
  ry += (ty - ry) * 0.14
  dxx += (tx - dxx) * 0.4  // 尾点跟得紧
  dyy += (ty - dyy) * 0.4
  if (ring.value) ring.value.style.transform = `translate(${rx - 16}px, ${ry - 16}px)`
  if (dot.value) dot.value.style.transform = `translate(${dxx - 3}px, ${dyy - 3}px)`
  raf = requestAnimationFrame(loop)
}

let cursorOn = false
function startCursor() {
  if (cursorOn) return
  cursorOn = true
  window.addEventListener('mousemove', onMove, { passive: true })
  loop()
}
function stopCursor() {
  cursorOn = false
  cancelAnimationFrame(raf)
  window.removeEventListener('mousemove', onMove)
}

onMounted(() => {
  if (window.matchMedia('(hover: hover)').matches && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    startCursor()
  }
})
onUnmounted(stopCursor)

// S92 用户反馈：演示账号 chip 一键填入（成果保留）
function fill(u, p) {
  username.value = u
  password.value = p
  error.value = ''
}

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名与口令'
    return
  }
  loading.value = true
  try {
    const res = await request.post('/auth/login', {
      username: username.value.trim(),
      password: password.value
    })
    saveAuth(res)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/overview'
    router.replace(redirect)
  } catch (e) {
    error.value = e.response?.data?.message
      || (e.response?.status === 401 ? '用户名或口令错误' : '登录失败，请检查服务是否可用')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- S101：粒子网络背景（主题自适应 + 光标引力响应） -->
    <ParticleField />

    <!-- S101 补充：自定义光标（光环弹性跟随 + 尾点；触屏/减少动效设备不启用） -->
    <div ref="ring" class="cursor-ring" aria-hidden="true"></div>
    <div ref="dot" class="cursor-dot" aria-hidden="true"></div>

    <div class="login-inner">
      <!-- 左：指挥雷达动效面板（常暗大屏风，与主题解耦） -->
      <aside class="login-hero">
        <div class="hero-brand">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="6.5"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>
          <span>空地协同巡检平台</span>
        </div>
        <p class="hero-sub">无人机 - 机器狗空地协同巡检集成平台 · 园区安防</p>

        <!-- 雷达 + 航迹动画（纯 CSS/SVG，prefers-reduced-motion 时静止） -->
        <div class="radar-panel">
          <div class="radar-status"><i class="blink"></i>SYSTEM ONLINE · 巡检网络在线</div>
          <div class="radar">
            <div class="radar-grid"></div>
            <div class="radar-sweep"></div>
            <span class="ping p1"></span>
            <span class="ping p2"></span>
            <span class="ping p3"></span>
            <svg class="radar-route" viewBox="0 0 260 200" fill="none">
              <path id="rt1" d="M20 170 L20 40 L130 40 L130 170 L240 170 L240 90 L150 90"
                    stroke="#38bdf8" stroke-opacity=".55" stroke-width="1.6" stroke-dasharray="7 7" class="flow"/>
              <path d="M20 170 L20 40 L130 40 L130 170 L240 170 L240 90 L150 90"
                    stroke="#38bdf8" stroke-opacity=".14" stroke-width="4" stroke-linecap="round"/>
              <circle r="4.5" fill="#38bdf8">
                <animateMotion dur="9s" repeatCount="indefinite" path="M20 170 L20 40 L130 40 L130 170 L240 170 L240 90 L150 90"/>
              </circle>
              <path id="rt2" d="M50 30 C90 10, 180 20, 235 55" stroke="#34d399" stroke-opacity=".55"
                    stroke-width="1.6" stroke-dasharray="6 6" class="flow2"/>
              <circle r="4" fill="#34d399">
                <animateMotion dur="6s" repeatCount="indefinite" path="M50 30 C90 10, 180 20, 235 55"/>
              </circle>
            </svg>
            <div class="radar-tag">UAV × 2 · ROBOT × 2 · 任务闭环中</div>
          </div>
        </div>

        <ul class="hero-points">
          <li><b>实时感知</b>——4 台设备心跳与遥测经 Kafka 流式入库，秒级上屏</li>
          <li><b>智能复核</b>——告警自动就近派单机器狗，红外复核回填证据链</li>
          <li><b>全链闭环</b>——任务下发 / 执行回执 / 日志归档 / 数据备份一体</li>
        </ul>
        <p class="hero-foot">HDFS · Kafka · MongoDB · Elasticsearch · Kibana · Nginx</p>
      </aside>

      <!-- 右：登录卡（入场动效） -->
      <form class="login-card" @submit.prevent="submit">
        <div class="login-brand">登录</div>
        <div class="login-sub">使用演示账号或分配的账号进入平台</div>

        <label class="login-field">
          <span>用户名</span>
          <input v-model="username" type="text" autocomplete="username" placeholder="admin" autofocus />
        </label>
        <label class="login-field">
          <span>口令</span>
          <input v-model="password" type="password" autocomplete="current-password" placeholder="••••••" />
        </label>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button class="login-btn" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <div class="login-demo">
          <span class="demo-title">演示账号（点击自动填入）</span>
          <button type="button" class="demo-chip" @click="fill('admin', 'admin123')">
            admin / admin123 · 系统管理员
          </button>
          <button type="button" class="demo-chip" @click="fill('operator', 'operator123')">
            operator / operator123 · 巡检值班员
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
  padding: 20px;
}

/* S101 补充：自定义光标（光环 + 尾点，不遮挡原生指针，仅可 hover 设备显示） */
.cursor-ring, .cursor-dot {
  position: fixed;
  top: 0;
  left: 0;
  border-radius: 50%;
  pointer-events: none;
  z-index: 9998;
  display: none;
}
.cursor-ring {
  width: 32px;
  height: 32px;
  border: 1.5px solid var(--accent);
  opacity: .55;
}
.cursor-dot {
  width: 6px;
  height: 6px;
  background: var(--accent);
  opacity: .9;
}
@media (hover: hover) and (prefers-reduced-motion: no-preference) {
  .cursor-ring, .cursor-dot { display: block; }
}
.login-inner {
  position: relative;
  z-index: 1;
  min-height: calc(100vh - 40px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 56px;
}

/* ---------- 左：品牌 + 雷达面板（常暗大屏风，主题解耦） ---------- */
.login-hero { max-width: 420px; color: var(--text-1); }
.hero-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-1);
}
.hero-brand svg { color: var(--accent); }
.hero-sub { margin-top: 10px; font-size: 13px; color: var(--text-2); }
@media (max-width: 920px) { .login-hero { display: none; } }

/* 雷达面板 */
.radar-panel {
  margin: 22px 0 20px;
  padding: 14px 16px 16px;
  border-radius: var(--radius-2);
  background: #0b1524;
  border: 1px solid #1e3a5f;
  box-shadow: 0 10px 34px rgba(2, 12, 27, 0.5);
}
.radar-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: #7dd3fc;
  font-family: var(--font-mono);
  margin-bottom: 12px;
}
.blink {
  width: 8px; height: 8px; border-radius: 50%;
  background: #34d399;
  animation: blink 1.6s ease-in-out infinite;
}
@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: .25; } }

.radar {
  position: relative;
  height: 220px;
  border-radius: var(--radius-1);
  overflow: hidden;
  background:
    linear-gradient(rgba(56, 189, 248, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.07) 1px, transparent 1px),
    radial-gradient(circle at 50% 50%, rgba(56, 189, 248, 0.10), transparent 65%),
    #0a1220;
  background-size: 26px 26px, 26px 26px, 100% 100%, 100% 100%;
}
/* 旋转扫描扇面 */
.radar-sweep {
  position: absolute;
  inset: -40%;
  background: conic-gradient(from 0deg, rgba(56, 189, 248, 0.35), rgba(56, 189, 248, 0.06) 55deg, transparent 90deg);
  animation: sweep 4.5s linear infinite;
}
@keyframes sweep { to { transform: rotate(360deg); } }
/* 目标闪现 */
.ping {
  position: absolute;
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #f87171;
  box-shadow: 0 0 0 0 rgba(248, 113, 113, 0.6);
  animation: ping 2.6s ease-out infinite;
}
.ping.p1 { left: 30%; top: 32%; }
.ping.p2 { left: 64%; top: 58%; animation-delay: .9s; }
.ping.p3 { left: 45%; top: 72%; animation-delay: 1.7s; }
@keyframes ping {
  0%   { box-shadow: 0 0 0 0 rgba(248, 113, 113, 0.55); }
  70%  { box-shadow: 0 0 0 14px rgba(248, 113, 113, 0); }
  100% { box-shadow: 0 0 0 0 rgba(248, 113, 113, 0); }
}
/* 航迹：虚线流动 + animateMotion 光点 */
.radar-route { position: absolute; inset: 10px; width: calc(100% - 20px); height: calc(100% - 20px); }
.radar-route .flow  { animation: dashflow 1.4s linear infinite; }
.radar-route .flow2 { animation: dashflow 1.2s linear infinite; }
@keyframes dashflow { to { stroke-dashoffset: -26; } }
.radar-tag {
  position: absolute;
  left: 10px;
  bottom: 8px;
  font-size: 11px;
  color: #7dd3fc;
  font-family: var(--font-mono);
  opacity: .85;
}

.hero-points { list-style: none; padding: 0; margin: 22px 0; display: flex; flex-direction: column; gap: 12px; }
.hero-points li {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.6;
  padding-left: 14px;
  border-left: 3px solid var(--accent);
}
.hero-points b { color: var(--text-1); margin-right: 4px; }
.hero-foot { font-size: 12px; color: var(--text-3); letter-spacing: .4px; }

/* ---------- 右：登录卡（入场动效 + 按钮扫光） ---------- */
.login-card {
  width: 372px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-2);
  padding: 30px 28px 22px;
  box-shadow: var(--shadow-2);
  display: flex;
  flex-direction: column;
  gap: 14px;
  animation: card-in .5s cubic-bezier(.2, .7, .3, 1) both;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(18px); }
  to   { opacity: 1; transform: none; }
}
.login-brand { font-size: 19px; font-weight: 700; text-align: center; color: var(--text-1); }
.login-sub { font-size: 12px; color: var(--text-3); text-align: center; margin-top: -6px; }
.login-field { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--text-2); }
.login-field input {
  padding: 11px 12px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-1);
  font-size: 14px;
  background: var(--bg-card);
  color: var(--text-1);
  outline: none;
  transition: border-color .15s, box-shadow .15s;
}
.login-field input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }
.login-error {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid transparent;
  border-radius: var(--radius-1);
  padding: 8px 10px;
  font-size: 13px;
}
/* 按钮：hover 扫光 */
.login-btn {
  position: relative;
  overflow: hidden;
  margin-top: 4px;
  padding: 12px;
  border: none;
  border-radius: var(--radius-1);
  background: var(--accent);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.login-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -70%;
  width: 45%;
  height: 100%;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.35), transparent);
  transform: skewX(-20deg);
  transition: left .45s ease;
}
.login-btn:hover:not(:disabled) { background: var(--accent-hover); }
.login-btn:hover:not(:disabled)::after { left: 130%; }
.login-btn:disabled { opacity: .6; cursor: not-allowed; }

/* S92：演示账号一键填入（成果保留） */
.login-demo {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}
.demo-title { font-size: 12px; color: var(--text-3); text-align: center; }
.demo-chip {
  padding: 8px 10px;
  font-size: 12px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid transparent;
  border-radius: var(--radius-1);
  cursor: pointer;
  transition: background .15s, transform .12s;
}
.demo-chip:hover { background: var(--bg-hover); transform: translateY(-1px); }

/* 无障碍：偏好减少动效时全部静止 */
@media (prefers-reduced-motion: reduce) {
  .radar-sweep, .blink, .ping, .radar-route .flow, .radar-route .flow2 { animation: none; }
  .login-card { animation: none; }
}
</style>
