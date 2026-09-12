import axios from 'axios'

// 统一 API 封装（设计报告 5.2.7(1)）：
// baseURL 为相对路径 /api，由 Nginx 转发——前端不感知后端实例与端口
const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.response.use(
  res => {
    const inst = res.headers['x-backend-instance']
    if (inst) window.__lastBackendInstance = inst
    return res.data
  },
  err => {
    const msg = err.response?.status === 503 ? '请求过于频繁，请稍后重试' : '服务异常，请稍后重试'
    console.error('[API]', err.config?.url, msg)
    return Promise.reject(err)
  }
)

export default request
