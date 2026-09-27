<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="主流程状态">
          <el-select v-model="q.status" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="s in store.enums?.eventStatus" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="应急状态">
          <el-select v-model="q.emergencyStatus" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="s in store.enums?.emergencyStatus" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="权属状态">
          <el-select v-model="q.ownershipStatus" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="s in store.enums?.ownershipStatus" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="q.keyword" placeholder="编号/位置" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button v-if="isCitizen" type="success" @click="$router.push('/report')">我要上报</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="store.user?.role === 'OWNER_UNIT'" type="warning" :closable="false" style="margin-bottom: 12px"
                title="列表包含：已确认给本单位的事件 + 权属待定可认领的事件" />

      <el-table :data="rows" v-loading="loading" @row-click="r => $router.push(`/events/${r.id}`)"
                row-style="cursor: pointer" stripe>
        <el-table-column prop="eventNo" label="事件编号" width="150" />
        <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
        <el-table-column prop="roadName" label="路段" width="100" />
        <el-table-column prop="problemType" label="问题" width="70" />
        <el-table-column label="危险程度" width="90">
          <template #default="{ row }">
            <el-tag :type="DANGER_TAG[row.dangerLevel]" size="small">{{ row.dangerLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="人车损伤" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.damageCaused" type="danger" size="small">有</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="应急处置线" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.emergencyStatus]" size="small">
              {{ store.label('emergencyStatus', row.emergencyStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权属追责线" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status]" size="small">
              {{ store.label('eventStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerUnitName" label="权属单位" width="110">
          <template #default="{ row }">{{ row.ownerUnitName || '待定' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上报时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { store, STATUS_TAG, DANGER_TAG } from '../store'

const rows = ref([])
const loading = ref(false)
const q = ref({ status: '', emergencyStatus: '', ownershipStatus: '', keyword: '' })

const isCitizen = computed(() => store.user?.role === 'CITIZEN')

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/events', { params: q.value })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
