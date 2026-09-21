import axios from 'axios'

// 统一 API 封装（设计报告 5.2.7(1)）：
// baseURL 为相对路径 /api，由 Nginx 转发——前端不感知后端实例与端口
const request = axios.create({ baseURL: '/api', timeout: 15000 })

// ===== S88：令牌存取（课程任务项 1 简单权限认证） =====
const TOKEN_KEY = 'inspection_token'
const USER_KEY = 'inspection_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}
export const saveAuth = (loginResult) => {
  localStorage.setItem(TOKEN_KEY, loginResult.token)
  localStorage.setItem(USER_KEY, JSON.stringify({
    username: loginResult.username,
    displayName: loginResult.displayName,
    role: loginResult.role,
    expiresAt: loginResult.expiresAt
  }))
}
export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

// 请求拦截：自动携带 Bearer 令牌
request.interceptors.request.use(cfg => {
  const token = getToken()
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

request.interceptors.response.use(
  res => {
    const inst = res.headers['x-backend-instance']
    if (inst) window.__lastBackendInstance = inst
    return res.data
  },
  err => {
    const status = err.response?.status
    const serverMsg = err.response?.data?.message

    // 401：清空登录态并跳登录页（带回跳地址）；已在登录页则不重复跳转
    if (status === 401) {
      clearAuth()
      const here = location.pathname + location.search
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login?redirect=' + encodeURIComponent(here)
      }
      console.error('[API] 401', err.config?.url, serverMsg || '未认证')
      return Promise.reject(err)
    }

    const msg = status === 503
      ? '请求过于频繁，请稍后重试'
      : (serverMsg || '服务异常，请稍后重试')
    console.error('[API]', err.config?.url, msg)
    return Promise.reject(err)
  }
)

export default request
