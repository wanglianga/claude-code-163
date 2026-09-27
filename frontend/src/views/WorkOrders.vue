<template>
  <el-card>
    <template #header><b>维修工单</b></template>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="工单号" width="160" />
      <el-table-column label="事件" width="130">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/events/${row.eventId}`)">事件 #{{ row.eventId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="ownerUnitName" label="权属单位" width="110" />
      <el-table-column prop="repairTeamName" label="维修队" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="STATUS_TAG[row.status]">{{ store.label('workOrderStatus', row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="派单时间" width="150"><template #default="{ row }">{{ fmt(row.claimedAt) }}</template></el-table-column>
      <el-table-column label="到场时间" width="150"><template #default="{ row }">{{ fmt(row.arrivedAt) }}</template></el-table-column>
      <el-table-column label="完工时间" width="150"><template #default="{ row }">{{ fmt(row.finishedAt) }}</template></el-table-column>
      <el-table-column label="用时(小时)" width="100">
        <template #default="{ row }">
          <span :style="{ color: hours(row) > 48 ? '#f56c6c' : 'inherit' }">{{ hours(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="coverSpec" label="井盖规格" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/events/${row.eventId}`)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { store, STATUS_TAG } from '../store'

const rows = ref([])
const loading = ref(false)

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '-' }
function hours(row) {
  if (!row.claimedAt) return '-'
  const end = row.finishedAt ? new Date(row.finishedAt) : new Date()
  return (Math.round((end - new Date(row.claimedAt)) / 360000) / 10).toFixed(1)
}

onMounted(async () => {
  loading.value = true
  try { rows.value = await api.get('/work-orders') } finally { loading.value = false }
})
</script>
