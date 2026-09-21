<script setup>
// S97-a：内联 SVG 图标组件（Lucide 图标集 path，MIT 协议，零运行时依赖）
// 替换原 emoji 导航图标（docs/前端视觉焕新方案.md §5.1）
const props = defineProps({
  name: { type: String, required: true },
  size: { type: Number, default: 18 }
})

// 每个图标 = 一组 <path d>（圆/矩形统一转写为 path，保持 stroke 风格一致）
const ICONS = {
  target: [
    'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z',
    'M12 2v3',
    'M12 19v3',
    'M2 12h3',
    'M19 12h3'
  ],
  map: [
    'M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3z',
    'M9 3v15',
    'M15 6v15'
  ],
  cpu: [
    'M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z',
    'M10 10h4v4h-4z',
    'M9 2v2',
    'M15 2v2',
    'M9 20v2',
    'M15 20v2',
    'M2 9h2',
    'M2 15h2',
    'M20 9h2',
    'M20 15h2'
  ],
  clipboard: [
    'M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2',
    'M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z',
    'M12 11h4',
    'M12 16h4',
    'M8 11h.01',
    'M8 16h.01'
  ],
  alarm: [
    'm21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z',
    'M12 9v4',
    'M12 17h.01'
  ],
  stats: [
    'M3 3v16a2 2 0 0 0 2 2h16',
    'M18 17V9',
    'M13 17V5',
    'M8 17v-3'
  ],
  logout: [
    'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4',
    'm16 17 5-5-5-5',
    'M21 12H9'
  ],
  sun: [
    'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z',
    'M12 2v2',
    'M12 20v2',
    'm4.93 4.93 1.41 1.41',
    'm17.66 17.66 1.41 1.41',
    'M2 12h2',
    'M20 12h2',
    'm6.34 17.66-1.41 1.41',
    'm19.07 4.93-1.41 1.41'
  ],
  moon: [
    'M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z'
  ],
  user: [
    'M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z',
    'M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2'
  ]
}
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    style="flex-shrink: 0"
  >
    <path v-for="(d, i) in ICONS[name] || []" :key="i" :d="d" />
  </svg>
</template>
