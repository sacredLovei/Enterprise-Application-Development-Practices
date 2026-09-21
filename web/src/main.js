import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { getTheme, applyTheme } from './utils/theme'
import './styles/tokens.css'   // S97：设计令牌必须最先加载（组件样式引用其变量）
import './style.css'

// S97-b：挂载前恢复上次主题，避免首帧闪烁（FOUC）
applyTheme(getTheme())

createApp(App).use(router).mount('#app')
