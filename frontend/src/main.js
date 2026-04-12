import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import { setupGlobalErrorMonitoring } from '@/utils/log-center'

const app = createApp(App)

setupGlobalErrorMonitoring(app)

app.use(createPinia()).use(router).use(Vant).mount('#app')
