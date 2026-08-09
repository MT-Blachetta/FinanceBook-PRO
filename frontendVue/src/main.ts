/**
 * Application entry point.
 *
 * Creates the Vue app, installs Pinia and Vue Router,
 * validates any stored auth token, then mounts to #app.
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './styles/global.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
