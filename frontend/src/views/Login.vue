<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">
        <h2>城市无主井盖协同治理平台</h2>
        <p>认领 · 巡检 · 维修 · 赔付</p>
      </div>
      <el-form @submit.prevent="doLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large"
                    :prefix-icon="Lock" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider>测试账号（点击填充）</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc" @click="fill(a)">{{ a.label }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { store } from '../store'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

const accounts = [
  { u: 'citizen1', p: '123456', label: '市民' },
  { u: 'street1', p: '123456', label: '街道值班员' },
  { u: 'drain', p: '123456', label: '权属单位' },
  { u: 'repair1', p: '123456', label: '维修队' },
  { u: 'inspector1', p: '123456', label: '巡查员' },
  { u: 'comp1', p: '123456', label: '赔付专员' },
  { u: 'admin', p: 'admin123', label: '管理员' }
]

function fill(a) {
  username.value = a.u
  password.value = a.p
}

async function doLogin() {
  if (!username.value || !password.value) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const data = await api.post('/auth/login', { username: username.value, password: password.value })
    store.setUser(data.user, data.token)
    store.enums = await api.get('/meta/enums')
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f3a5f 0%, #2e5c8a 100%); }
.login-card { width: 420px; }
.title { text-align: center; margin-bottom: 24px; }
.title h2 { font-size: 20px; color: #1f3a5f; }
.title p { color: #909399; margin-top: 6px; font-size: 13px; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.acc { cursor: pointer; }
</style>
