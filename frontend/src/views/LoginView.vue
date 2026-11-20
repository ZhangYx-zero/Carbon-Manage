<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456'
})

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data: any = await login(form)
    userStore.setLogin(data)
    ElMessage.success('登录成功')
    router.push(userStore.isOperator ? '/orders' : '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">企业碳资产智能监测系统</div>
      <div class="login-subtitle">监测 · 诊断 · 整改 一体化碳管理平台</div>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" size="large" placeholder="请输入账号" @keyup.enter="submit">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="submit"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>
      <div class="login-tip">管理员：admin / 123456</div>
      <div class="login-tip">整改负责人：operator1 / 123456</div>
    </div>
  </div>
</template>
