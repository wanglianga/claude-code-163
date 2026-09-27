<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <b>费用台账与复核分摊</b>
        <el-checkbox v-model="onlyPending" @change="load">仅看责任待定</el-checkbox>
      </div>
    </template>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="事件" width="110">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/events/${row.eventId}`)">事件 #{{ row.eventId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="costType" label="费用类型" width="110" />
      <el-table-column prop="amount" label="金额(元)" width="110" />
      <el-table-column label="承担状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.bearerStatus === 'PENDING' ? 'danger' : 'success'">
            {{ row.bearerStatus === 'PENDING' ? '责任待定' : '已分摊' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="承担方" width="140">
        <template #default="{ row }">
          {{ row.bearerUnitName || { OWNER: '权属单位', STREET: '街道', SHARED: '多方分摊' }[row.bearerType] || '待定' }}
        </template>
      </el-table-column>
      <el-table-column prop="note" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column label="入账时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.bearerStatus === 'PENDING'" size="small" type="primary" @click="openAllocate(row)">复核分摊</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" title="费用复核分摊" width="460px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
                :title="`费用：${current?.costType} ${current?.amount} 元（事件 #${current?.eventId}）`" />
      <el-form label-width="90px">
        <el-form-item label="承担方" required>
          <el-radio-group v-model="form.bearerType">
            <el-radio value="OWNER">权属单位</el-radio>
            <el-radio value="STREET">街道</el-radio>
            <el-radio value="SHARED">多方分摊</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.bearerType === 'OWNER'" label="权属单位" required>
          <el-select v-model="form.bearerUnitId" style="width: 100%">
            <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="分摊依据，如复核确认的权属结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="doAllocate">确认分摊</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const rows = ref([])
const units = ref([])
const loading = ref(false)
const onlyPending = ref(false)
const dlg = ref(false)
const current = ref(null)
const form = reactive({ bearerType: 'OWNER', bearerUnitId: null, note: '' })

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/costs', { params: onlyPending.value ? { bearerStatus: 'PENDING' } : {} })
  } finally {
    loading.value = false
  }
}

function openAllocate(row) {
  current.value = row
  dlg.value = true
}

async function doAllocate() {
  await api.post(`/costs/${current.value.id}/allocate`, form)
  ElMessage.success('费用已分摊')
  dlg.value = false
  load()
}

onMounted(async () => {
  load()
  units.value = await api.get('/meta/units')
})
</script>
