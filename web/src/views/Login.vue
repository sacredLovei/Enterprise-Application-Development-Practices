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
  <div class="login-page">
    <form class="login-card" @submit.prevent="submit">
      <div class="login-brand">🚁 空地协同巡检平台</div>
      <div class="login-sub">无人机 - 机器狗空地协同巡检集成平台 · 园区安防</div>

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
  background: linear-gradient(135deg, #eef1f6 0%, #dfe8f6 100%);
  padding: 20px;
}
.login-card {
  width: 372px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(18px);
  border: 1px solid #e2e7ee;
  border-radius: 16px;
  padding: 30px 28px 22px;
  box-shadow: 0 18px 44px rgba(36, 48, 60, 0.10);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.login-brand { font-size: 19px; font-weight: 700; text-align: center; }
.login-sub { font-size: 12px; color: #8b98a6; text-align: center; margin-top: -6px; }
.login-field { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #5c6b7a; }
.login-field input {
  padding: 11px 12px;
  border: 1px solid #d8dfe8;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color .15s, box-shadow .15s;
}
.login-field input:focus { border-color: #2f6fed; box-shadow: 0 0 0 3px rgba(47, 111, 237, 0.12); }
.login-error {
  background: #fdecec;
  color: #c0392b;
  border: 1px solid #f6d0cc;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
}
.login-btn {
  margin-top: 4px;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #2f6fed;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.login-btn:hover:not(:disabled) { background: #245ccd; }
.login-btn:disabled { opacity: .6; cursor: not-allowed; }
.login-hint { font-size: 12px; color: #9aa7b5; text-align: center; }

/* S92：演示账号一键填入 */
.login-demo {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed #e2e7ee;
}
.demo-title { font-size: 12px; color: #9aa7b5; text-align: center; }
.demo-chip {
  padding: 8px 10px;
  font-size: 12px;
  color: #2f6fed;
  background: #f2f6fe;
  border: 1px solid #d8e3fb;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s, border-color .15s;
}
.demo-chip:hover { background: #e6effd; border-color: #b9cdf7; }
</style>
