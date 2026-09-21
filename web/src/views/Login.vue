<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request, { saveAuth } from '../api/request'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

// S92 用户反馈：演示账号提示原为纯文本，点击不会填入表单（用户误以为点了就能登录）。
// 现改为可点击的 chip，一键填入用户名与口令。
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
  <!-- S97-d：左右分栏——左侧品牌区（窄屏自动隐藏），右侧登录卡（S92 chip 成果保留） -->
  <div class="login-page">
    <aside class="login-hero">
      <div class="hero-brand">
        <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="6.5"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>
        <span>空地协同巡检平台</span>
      </div>
      <p class="hero-sub">无人机 - 机器狗空地协同巡检集成平台 · 园区安防</p>
      <ul class="hero-points">
        <li><b>实时感知</b>——4 台设备心跳与遥测经 Kafka 流式入库，秒级上屏</li>
        <li><b>智能复核</b>——告警自动就近派单机器狗，红外复核回填证据链</li>
        <li><b>全链闭环</b>——任务下发 / 执行回执 / 日志归档 / 数据备份一体</li>
      </ul>
      <p class="hero-foot">HDFS · Kafka · MongoDB · Elasticsearch · Kibana · Nginx</p>
    </aside>

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
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 56px;
  background: var(--bg-page);
  padding: 20px;
}

/* 左侧品牌区：克制配色，不抢登录卡视觉 */
.login-hero { max-width: 400px; color: var(--text-1); }
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
.hero-points { list-style: none; padding: 0; margin: 26px 0; display: flex; flex-direction: column; gap: 14px; }
.hero-points li {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.6;
  padding-left: 14px;
  border-left: 3px solid var(--accent);
}
.hero-points b { color: var(--text-1); margin-right: 4px; }
.hero-foot { font-size: 12px; color: var(--text-3); letter-spacing: .4px; }
@media (max-width: 860px) { .login-hero { display: none; } }

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
.login-btn {
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
.login-btn:hover:not(:disabled) { background: var(--accent-hover); }
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
  transition: background .15s;
}
.demo-chip:hover { background: var(--bg-hover); }
</style>
