<template>
  <div v-loading="loading">
    <!-- 总览卡片 -->
    <el-row :gutter="16" v-if="ov">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card class="stat-card">
          <div class="stat-value" :style="{ color: c.color }">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card><template #header><b>事件状态分布</b></template><div ref="statusChart" class="chart"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card><template #header><b>高发路段 TOP</b></template><div ref="roadChart" class="chart"></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header><b>权属争议单位</b></template>
          <el-table :data="disputed" size="small">
            <el-table-column prop="unitName" label="权属单位" width="110" />
            <el-table-column prop="unitType" label="类型" width="70" />
            <el-table-column prop="disputeCount" label="争议次数" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.disputeCount > 0" type="danger" size="small">{{ row.disputeCount }}</el-tag>
                <span v-else>0</span>
              </template>
            </el-table-column>
            <el-table-column prop="eventCount" label="涉及事件" width="90" />
            <el-table-column prop="avgConfirmHours" label="平均确认(小时)" width="120" />
            <el-table-column prop="compensationTotal" label="赔付金额(元)" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><b>维修超时工单（SLA 48小时）</b></template>
          <el-table :data="timeouts" size="small">
            <el-table-column prop="orderNo" label="工单号" width="150" />
            <el-table-column prop="ownerUnitName" label="权属单位" width="100" />
            <el-table-column prop="repairTeamName" label="维修队" width="110" />
            <el-table-column prop="hours" label="用时(小时)" width="100">
              <template #default="{ row }"><span style="color: #f56c6c">{{ row.hours }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">{{ row.finished ? '已完工' : '未完工' }}</template>
            </el-table-column>
            <el-table-column label="事件" width="80">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push(`/events/${row.eventId}`)">查看</el-link>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card><template #header><b>赔付金额分布（按承担方）</b></template><div ref="compChart" class="chart"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><b>治理建议（基于沉淀数据）</b></template>
          <el-timeline style="padding: 8px 0 0 6px">
            <el-timeline-item v-if="hotRoads[0]" type="danger">
              高发路段「{{ hotRoads[0].roadName }}」已发生 {{ hotRoads[0].count }} 起事件，建议加密日常巡检路线频次
            </el-timeline-item>
            <el-timeline-item v-for="u in disputed.filter(x => x.disputeCount > 0)" :key="u.unitId" type="warning">
              「{{ u.unitName }}」存在 {{ u.disputeCount }} 次权属争议，建议核实施工移交规则与台账登记
            </el-timeline-item>
            <el-timeline-item v-if="timeouts.length" type="warning">
              {{ timeouts.length }} 个工单维修超时，建议约谈权属单位与维修队，纳入考核
            </el-timeline-item>
            <el-timeline-item v-if="ov && ov.pendingCosts > 0" type="primary">
              {{ ov.pendingCosts }} 笔费用责任待定，请尽快复核分摊，避免长期挂账
            </el-timeline-item>
            <el-timeline-item v-if="ov">
              平均应急到场时效 {{ ov.avgArrivalMinutes }} 分钟（SLA 120 分钟）
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../api'
import { store } from '../store'

const loading = ref(false)
const ov = ref(null)
const hotRoads = ref([])
const disputed = ref([])
const timeouts = ref([])
const compData = ref(null)

const statusChart = ref(null)
const roadChart = ref(null)
const compChart = ref(null)

const cards = computed(() => ov.value ? [
  { label: '事件总数', value: ov.value.totalEvents, color: '#409eff' },
  { label: '待应急处置', value: ov.value.pendingEmergency, color: '#e6a23c' },
  { label: '权属待定', value: ov.value.undetermined, color: '#f56c6c' },
  { label: '平均到场时效(分钟)', value: ov.value.avgArrivalMinutes, color: '#67c23a' },
  { label: '维修超时工单', value: ov.value.repairTimeouts, color: '#f56c6c' },
  { label: '赔付总额(元)', value: ov.value.totalCompensation, color: '#e6a23c' },
  { label: '待分摊费用(笔)', value: ov.value.pendingCosts, color: '#f56c6c' },
  { label: '高危事件', value: ov.value.byDanger?.['高'] || 0, color: '#f56c6c' }
] : [])

function pie(el, title, data) {
  echarts.init(el).setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['40%', '70%'], data, label: { formatter: '{b}: {c}' } }]
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const [o, r, du, to, cp] = await Promise.all([
      api.get('/stats/overview'), api.get('/stats/hot-roads'),
      api.get('/stats/disputed-units'), api.get('/stats/repair-timeouts'),
      api.get('/stats/compensations')
    ])
    ov.value = o; hotRoads.value = r; disputed.value = du; timeouts.value = to; compData.value = cp
    await nextTick()
    const statusLabel = v => store.label('eventStatus', v)
    pie(statusChart.value, '状态', Object.entries(o.byStatus).map(([k, v]) => ({ name: statusLabel(k), value: v })))
    echarts.init(roadChart.value).setOption({
      tooltip: {},
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: r.slice(0, 8).map(x => x.roadName).reverse() },
      series: [{ type: 'bar', data: r.slice(0, 8).map(x => x.count).reverse(), itemStyle: { color: '#409eff' }, label: { show: true, position: 'right' } }]
    })
    pie(compChart.value, '赔付', Object.entries(cp.byUnit).map(([k, v]) => ({ name: k, value: v })))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-value { font-size: 26px; font-weight: 700; }
.stat-label { color: #909399; font-size: 12px; margin-top: 6px; }
.chart { height: 280px; }
</style>
