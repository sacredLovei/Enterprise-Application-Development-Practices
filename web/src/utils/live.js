// S98：设备实时位置流客户端（fetch 流式读取 SSE）
//
// 为什么不用原生 EventSource：它无法自定义请求头，而 /api/stream/** 需要
// Bearer 令牌（S88 鉴权）；把令牌放 URL 查询参数会进浏览器历史与日志（S96 同一口径）。
// 因此用 fetch + ReadableStream 手工解析 SSE 帧（data: 行 + 空行分帧）。
import { getToken } from '../api/request'

/**
 * 连接实时位置流。
 * @param {(msg: {ts:number, devices:Array}) => void} onEvent 收到 positions 事件
 * @param {('open'|'closed'|'error')} onState 连接状态变化（error/closed 后内部自动 5s 重连）
 * @returns {() => void} 断开函数（组件卸载时调用，之后不再重连）
 */
export function connectPositions(onEvent, onState) {
  let closed = false
  let retryTimer = null
  const ctrl = new AbortController()

  async function loop() {
    while (!closed) {
      try {
        const res = await fetch('/api/stream/positions', {
          headers: { Authorization: 'Bearer ' + getToken() },
          signal: ctrl.signal
        })
        if (!res.ok || !res.body) throw new Error('SSE HTTP ' + res.status)
        onState('open')
        const reader = res.body.getReader()
        const dec = new TextDecoder()
        let buf = ''
        for (;;) {
          const { done, value } = await reader.read()
          if (done) break
          buf += dec.decode(value, { stream: true })
          let idx
          while ((idx = buf.indexOf('\n\n')) >= 0) {
            const frame = buf.slice(0, idx)
            buf = buf.slice(idx + 2)
            const data = frame.split('\n')
              .filter(l => l.startsWith('data:'))
              .map(l => l.slice(5).trim())
              .join('\n')
            if (!data) continue   // 注释心跳帧（": keep-alive"）无 data 行
            try { onEvent(JSON.parse(data)) } catch (e) { /* 非 JSON 帧，忽略 */ }
          }
        }
        onState('closed')
      } catch (e) {
        if (closed || e?.name === 'AbortError') return
        onState('error')
      }
      if (!closed) {
        // 断线 5 秒重连（简单退避即可——服务端心跳密集，断连多为网关重启等罕见场景）
        await new Promise(r => { retryTimer = setTimeout(r, 5000) })
      }
    }
  }

  loop()

  return () => {
    closed = true
    clearTimeout(retryTimer)
    ctrl.abort()
  }
}
