import { createApp } from 'vue'
import {
  ElAlert, ElButton, ElCard, ElConfigProvider, ElDatePicker, ElDialog, ElForm, ElFormItem,
  ElInput, ElInputNumber, ElLoading, ElOption, ElPagination, ElSelect, ElSkeleton, ElTable,
  ElTableColumn, ElTag,
} from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import './style.css'

const app = createApp(App)
for (const component of [
  ElAlert, ElButton, ElCard, ElConfigProvider, ElDatePicker, ElDialog, ElForm, ElFormItem,
  ElInput, ElInputNumber, ElOption, ElPagination, ElSelect, ElSkeleton, ElTable, ElTableColumn, ElTag,
]) {
  app.component(component.name, component)
}
app.use(ElLoading)
app.mount('#app')
