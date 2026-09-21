import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'   // S97：设计令牌必须最先加载（组件样式引用其变量）
import './style.css'

createApp(App).use(router).mount('#app')
