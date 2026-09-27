<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <div class="logo-title">井盖协同治理</div>
        <div class="logo-sub">无主井盖认领 · 巡检 · 维修 · 赔付</div>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9"
               active-text-color="#409eff">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="crumb">{{ $route.meta.title || '' }}</div>
        <div class="user">
          <el-tag size="small" effect="dark">{{ store.roleLabel() }}</el-tag>
          <span class="name">{{ store.user?.name }}</span>
          <span class="org" v-if="store.user?.orgName">{{ store.user.orgName }}</span>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Grid, EditPen, Tickets, Coin, Money, DataAnalysis, Setting } from '@element-plus/icons-vue'
import { store } from '../store'
import api from '../api'

const router = useRouter()

onMounted(async () => {
  if (!store.enums) store.enums = await api.get('/meta/enums')
})

const ALL_MENUS = [
  { path: '/events', title: '事件中心', icon: Grid, roles: null },
  { path: '/report', title: '市民上报', icon: EditPen, roles: ['CITIZEN', 'ADMIN'] },
  { path: '/work-orders', title: '维修工单', icon: Tickets, roles: ['REPAIR_TEAM', 'OWNER_UNIT', 'ADMIN', 'STREET_DUTY', 'INSPECTOR'] },
  { path: '/compensations', title: '赔付案件', icon: Coin, roles: ['COMPENSATION', 'ADMIN', 'CITIZEN'] },
  { path: '/costs', title: '费用复核', icon: Money, roles: ['ADMIN', 'STREET_DUTY'] },
  { path: '/stats', title: '统计分析', icon: DataAnalysis, roles: null },
  { path: '/admin', title: '系统管理', icon: Setting, roles: ['ADMIN'] }
]

const menus = computed(() =>
  ALL_MENUS.filter(m => !m.roles || m.roles.includes(store.user?.role))
)

async function logout() {
  try { await api.post('/auth/logout') } catch (e) { /* 忽略 */ }
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #1f2d3d; }
.logo { padding: 18px 16px; border-bottom: 1px solid #2d3a4a; }
.logo-title { color: #fff; font-size: 17px; font-weight: 600; }
.logo-sub { color: #6b7c8d; font-size: 11px; margin-top: 4px; }
.aside :deep(.el-menu) { border-right: none; }
.header { background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed; }
.crumb { font-size: 15px; font-weight: 600; color: #303133; }
.user { display: flex; align-items: center; gap: 10px; }
.name { font-weight: 600; }
.org { color: #909399; font-size: 12px; }
.main { padding: 16px; }
</style>
