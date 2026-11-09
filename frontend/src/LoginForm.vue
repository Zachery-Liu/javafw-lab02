<script setup>
import { reactive, ref } from 'vue'
import { api } from './api.js'

const emit = defineEmits(['signed-in'])
const form = ref()
const credentials = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const busy = ref(false)
const error = ref('')

async function login() {
  if (busy.value || !await form.value.validate().catch(() => false)) return

  busy.value = true
  error.value = ''
  try {
    await api('login', { method: 'POST', body: credentials })
    emit('signed-in')
  } catch (failure) {
    error.value = failure.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <el-card class="login-card" shadow="never">
    <template #header><h1>登录</h1></template>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="form-alert" />
    <el-form ref="form" :model="credentials" :rules="rules" label-position="top" @submit.prevent="login">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="credentials.username" autocomplete="username" maxlength="128" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="credentials.password" type="password" autocomplete="current-password"
                  maxlength="256" show-password />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="busy" class="login-submit">登录</el-button>
    </el-form>
  </el-card>
</template>
