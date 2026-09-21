<script setup>
// S101：登录页粒子网络背景——Canvas 绘制，主题自适应（颜色取自 tokens.css 变量），
// rAF 驱动、resize 自适应、组件卸载清理；prefers-reduced-motion 时静态呈现不动画。
// S101 补充：光标动效——粒子被光标轻微牵引 + 光标附近粒子向光标连线 + 光标光晕。
import { ref, onMounted, onUnmounted } from 'vue'

const canvasEl = ref(null)
let raf = 0
let cleanup = null

onMounted(() => {
  const canvas = canvasEl.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  let w = 0, h = 0, dpr = Math.min(window.devicePixelRatio || 1, 2)
  function resize() {
    w = canvas.clientWidth
    h = canvas.clientHeight
    canvas.width = w * dpr
    canvas.height = h * dpr
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()
  window.addEventListener('resize', resize)

  // 光标位置（相对 canvas；离开页面时置 null）
  const mouse = { x: -9999, y: -9999, active: false }
  function onMove(e) {
    const r = canvas.getBoundingClientRect()
    mouse.x = e.clientX - r.left
    mouse.y = e.clientY - r.top
    mouse.active = mouse.x >= 0 && mouse.y >= 0 && mouse.x <= w && mouse.y <= h
  }
  function onLeave() { mouse.active = false }
  window.addEventListener('mousemove', onMove, { passive: true })
  document.documentElement.addEventListener('mouseleave', onLeave)

  // 主题色：从 CSS 变量读取（切主题时重读即可自适应）
  function themeColors() {
    const cs = getComputedStyle(document.documentElement)
    return {
      dot: cs.getPropertyValue('--accent').trim() || '#2563eb',
      line: cs.getPropertyValue('--text-4').trim() || '#c3cad4'
    }
  }

  // 粒子群：数量随面积，移动缓慢
  const N = 60
  const pts = Array.from({ length: N }, () => ({
    x: Math.random(),
    y: Math.random(),
    vx: (Math.random() - 0.5) * 0.00045,
    vy: (Math.random() - 0.5) * 0.00045,
    r: 1 + Math.random() * 1.6
  }))

  const LINK = 130      // 连线距离阈值（px）
  const MOUSE_R = 170   // 光标影响半径

  function frame() {
    const { dot, line } = themeColors()
    ctx.clearRect(0, 0, w, h)
    for (const p of pts) {
      // 光标引力：影响半径内的粒子被轻微牵引（不改变速度上限，弹性回落）
      if (mouse.active) {
        const dx = mouse.x - p.x * w
        const dy = mouse.y - p.y * h
        const d = Math.hypot(dx, dy)
        if (d < MOUSE_R && d > 1) {
          const f = (1 - d / MOUSE_R) * 0.0006
          p.vx += (dx / d) * f
          p.vy += (dy / d) * f
        }
      }
      // 限速，防止引力累积后飞散
      p.vx = Math.max(-0.0012, Math.min(0.0012, p.vx))
      p.vy = Math.max(-0.0012, Math.min(0.0012, p.vy))
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > 1) p.vx *= -1
      if (p.y < 0 || p.y > 1) p.vy *= -1
    }
    // 连线
    ctx.lineWidth = 0.6
    for (let i = 0; i < pts.length; i++) {
      for (let j = i + 1; j < pts.length; j++) {
        const a = pts[i], b = pts[j]
        const dx = (a.x - b.x) * w, dy = (a.y - b.y) * h
        const d = Math.hypot(dx, dy)
        if (d < LINK) {
          ctx.globalAlpha = (1 - d / LINK) * 0.28
          ctx.strokeStyle = line
          ctx.beginPath()
          ctx.moveTo(a.x * w, a.y * h)
          ctx.lineTo(b.x * w, b.y * h)
          ctx.stroke()
        }
      }
    }
    // 光标：仅向附近粒子连线（用户反馈：不要光晕色块与中心点）
    if (mouse.active) {
      ctx.lineWidth = 0.9
      for (const p of pts) {
        const dx = mouse.x - p.x * w, dy = mouse.y - p.y * h
        const d = Math.hypot(dx, dy)
        if (d < MOUSE_R) {
          ctx.globalAlpha = (1 - d / MOUSE_R) * 0.5
          ctx.strokeStyle = dot
          ctx.beginPath()
          ctx.moveTo(mouse.x, mouse.y)
          ctx.lineTo(p.x * w, p.y * h)
          ctx.stroke()
        }
      }
    }
    // 粒子
    ctx.globalAlpha = 0.55
    ctx.fillStyle = dot
    for (const p of pts) {
      ctx.beginPath()
      ctx.arc(p.x * w, p.y * h, p.r, 0, Math.PI * 2)
      ctx.fill()
    }
    ctx.globalAlpha = 1
    if (!reduced) raf = requestAnimationFrame(frame)
  }
  frame()   // reduced-motion 时只画一帧静态

  cleanup = () => {
    cancelAnimationFrame(raf)
    window.removeEventListener('resize', resize)
    window.removeEventListener('mousemove', onMove)
    document.documentElement.removeEventListener('mouseleave', onLeave)
  }
})

onUnmounted(() => { if (cleanup) cleanup() })
</script>

<template>
  <canvas ref="canvasEl" class="particle-field" aria-hidden="true"></canvas>
</template>

<style scoped>
.particle-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
