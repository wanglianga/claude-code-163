<template>
  <el-card>
    <template #header><b>赔付案件</b></template>
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="caseNo" label="案件号" width="160" />
      <el-table-column label="事件" width="110">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/events/${row.eventId}`)">事件 #{{ row.eventId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="claimantName" label="索赔人" width="90" />
      <el-table-column prop="claimantType" label="类型" width="100" />
      <el-table-column prop="claimAmount" label="索赔金额(元)" width="110" />
      <el-table-column prop="decidedAmount" label="核定金额(元)" width="110" />
      <el-table-column prop="responsibility" label="责任判定" width="110">
        <template #default="{ row }">{{ row.responsibility || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="STATUS_TAG[row.status]">{{ store.label('compensationStatus', row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="viewEvidence(row)">证据链</el-button>
          <el-button v-if="canDecide && row.status === 'PENDING'" size="small" type="primary" @click="openDecide(row)">判定</el-button>
          <el-button v-if="canDecide && row.status === 'DECIDED'" size="small" type="success" @click="doPay(row)">确认赔付</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 证据链 -->
    <el-dialog v-model="evidenceDlg" title="责任判定证据链" width="560px">
      <el-descriptions :column="1" border v-if="evidence">
        <el-descriptions-item v-for="(v, k) in evidence" :key="k" :label="k">
          {{ typeof v === 'string' && v.includes('T') && !isNaN(Date.parse(v)) ? v.replace('T', ' ').slice(0, 16) : v }}
        </el-descriptions-item>
      </el-descriptions>
      <el-alert type="info" :closable="false" style="margin-top: 12px"
                title="判定依据：上报时间、临时围护时效、权属确认耗时、维修时长与事故证明" />
    </el-dialog>

    <!-- 判定 -->
    <el-dialog v-model="decideDlg" title="赔付责任判定" width="520px">
      <el-form label-width="100px">
        <el-form-item label="责任判定" required>
          <el-radio-group v-model="decideForm.responsibility">
            <el-radio v-for="t in store.enums?.responsibilities" :key="t" :value="t">{{ t }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="核定金额" required>
          <el-input-number v-model="decideForm.decidedAmount" :min="0" :precision="2" /> 元
        </el-form-item>
        <el-form-item v-if="decideForm.responsibility === '权属单位'" label="承担单位">
          <el-select v-model="decideForm.bearerUnitId" clearable placeholder="默认事件权属单位" style="width: 100%">
            <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="判定理由">
          <el-input v-model="decideForm.responsibilityNote" type="textarea" :rows="3"
                    placeholder="依据围护时效、权属确认、维修时长、事故证明说明理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decideDlg = false">取消</el-button>
        <el-button type="primary" @click="doDecide">提交判定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { store, STATUS_TAG } from '../store'

const rows = ref([])
const loading = ref(false)
const units = ref([])
const evidenceDlg = ref(false)
const decideDlg = ref(false)
const evidence = ref(null)
const current = ref(null)

const decideForm = reactive({ responsibility: '权属单位', decidedAmount: 0, bearerUnitId: null, responsibilityNote: '' })

const canDecide = computed(() => ['COMPENSATION', 'ADMIN'].includes(store.user?.role))

async function load() {
  loading.value = true
  try { rows.value = await api.get('/compensations') } finally { loading.value = false }
}

async function viewEvidence(row) {
  const data = await api.get(`/compensations/${row.id}`)
  evidence.value = data.evidence
  evidenceDlg.value = true
}

function openDecide(row) {
  current.value = row
  decideForm.decidedAmount = row.claimAmount
  decideDlg.value = true
}

async function doDecide() {
  await api.post(`/compensations/${current.value.id}/decide`, decideForm)
  ElMessage.success('判定完成')
  decideDlg.value = false
  load()
}

async function doPay(row) {
  await ElMessageBox.confirm(`确认已向 ${row.claimantName} 赔付 ${row.decidedAmount} 元？`, '赔付确认', { type: 'warning' })
  await api.post(`/compensations/${row.id}/pay`)
  ElMessage.success('赔付完成')
  load()
}

onMounted(async () => {
  load()
  units.value = await api.get('/meta/units')
})
</script>
