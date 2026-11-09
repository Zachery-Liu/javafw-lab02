<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { api } from './api.js'
import EmployeeView from './EmployeeView.vue'
import LoginForm from './LoginForm.vue'

const starting = ref(true)
const authenticated = ref(false)
const signingOut = ref(false)
const initialPage = ref(null)

onMounted(async () => {
  try {
    initialPage.value = await api('emps')
    authenticated.value = true
  } catch (error) {
    if (error.status !== 401) ElMessage.error(error.message)
  } finally {
    starting.value = false
  }
})

function endSession() {
  authenticated.value = false
  initialPage.value = null
}

function sessionExpired() {
  endSession()
  ElMessage.warning('登录已失效，请重新登录')
}

async function logout() {
  signingOut.value = true
  try {
    await api('logout', { method: 'POST' })
    endSession()
  } catch (error) {
    if (error.status === 401) endSession()
    else ElMessage.error(error.message)
  } finally {
    signingOut.value = false
  }
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <header class="topbar">
      <div class="topbar-inner">
        <span class="brand">员工管理系统</span>
        <el-button v-if="authenticated" :loading="signingOut" @click="logout">退出登录</el-button>
      </div>
    </header>
    <main class="page">
      <el-card v-if="starting" class="login-card" shadow="never">
        <el-skeleton :rows="2" animated />
      </el-card>
      <EmployeeView v-else-if="authenticated" :initial-page="initialPage" @session-expired="sessionExpired" />
      <LoginForm v-else @signed-in="authenticated = true" />
    </main>
  </el-config-provider>
</template>
