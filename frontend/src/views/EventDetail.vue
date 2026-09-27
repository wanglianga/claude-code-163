<template>
  <div v-loading="loading">
    <template v-if="d.event">
      <!-- 基本信息 -->
      <el-card>
        <template #header>
          <div class="card-header">
            <b>{{ d.event.eventNo }}</b>
            <div>
              <el-tag :type="DANGER_TAG[d.event.dangerLevel]" style="margin-right: 8px">危险程度: {{ d.event.dangerLevel }}</el-tag>
              <el-tag v-if="d.event.damageCaused" type="danger">已造成人车损伤</el-tag>
            </div>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="位置">{{ d.event.location }}</el-descriptions-item>
          <el-descriptions-item label="路段">{{ d.event.roadName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="道路类型">{{ d.event.roadType }}</el-descriptions-item>
          <el-descriptions-item label="问题类型">{{ d.event.problemType }}</el-descriptions-item>
          <el-descriptions-item label="经纬度">
            {{ d.event.lng ? d.event.lng + ', ' + d.event.lat : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="上报人">{{ d.event.reporterName }}（{{ fmt(d.event.createdAt) }}）</el-descriptions-item>
          <el-descriptions-item label="权属单位">
            <b v-if="d.event.ownerUnitName">{{ d.event.ownerUnitName }}</b>
            <el-tag v-else type="danger" size="small">责任待定</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权属确认时间">{{ fmt(d.event.ownerConfirmedAt) }}</el-descriptions-item>
          <el-descriptions-item label="损伤情况">{{ d.event.damageDesc || '无' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="photoList.length" class="photos">
          <el-image v-for="(p, i) in photoList" :key="i" :src="p" :preview-src-list="photoList"
                    fit="cover" style="width: 110px; height: 110px; border-radius: 4px" preview-teleported />
        </div>
      </el-card>

      <!-- 两条线进度 -->
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="12">
          <el-card>
            <template #header><b>应急处置线</b><span class="line-desc">（只看现场风险与到场时效）</span></template>
            <el-steps :active="emergencyStep" align-center finish-status="success">
              <el-step title="市民上报" :description="fmt(d.event.createdAt)" />
              <el-step title="街道派发" :description="measure?.dispatchedAt ? fmt(measure.dispatchedAt) : '待派发'" />
              <el-step title="临时围护" :description="measure?.completedAt ? fmt(measure.completedAt) : '未完成'" />
            </el-steps>
            <div v-if="measure?.arrivedAt" class="arrival">
              到场时效：<b>{{ arrivalMinutes }}</b> 分钟
              <el-tag :type="arrivalMinutes <= 120 ? 'success' : 'danger'" size="small">
                {{ arrivalMinutes <= 120 ? '及时' : '超时' }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header><b>权属追责线</b><span class="line-desc">（看历史移交、施工、管线与巡检证据）</span></template>
            <el-steps :active="ownerStep" align-center finish-status="success"
                      :process-status="d.event.ownershipStatus === 'UNDETERMINED' ? 'error' : 'process'">
              <el-step title="权属排查" />
              <el-step :title="d.event.ownershipStatus === 'UNDETERMINED' ? '权属待定' : '权属确认'" />
              <el-step title="工单维修" />
              <el-step title="巡查验收" />
              <el-step title="结案" />
            </el-steps>
          </el-card>
        </el-col>
      </el-row>

      <!-- 操作区 -->
      <el-card style="margin-top: 16px" v-if="actions.length">
        <template #header><b>可执行操作</b></template>
        <el-button v-if="can('dispatch')" type="warning" @click="doDispatch">受理并派发应急处置</el-button>
        <el-button v-if="can('secure')" type="warning" @click="dlg.secure = true">登记临时围护</el-button>
        <el-button v-if="can('investigate')" type="primary" @click="dlg.investigate = true">提交权属排查</el-button>
        <el-button v-if="can('claim')" type="primary" @click="doClaim">本单位认领</el-button>
        <el-button v-if="can('order')" type="primary" @click="dlg.order = true">派发维修工单</el-button>
        <el-button v-if="can('case')" type="danger" @click="dlg.case = true">赔付立案</el-button>
        <el-button v-if="can('close')" type="success" @click="doClose">结案归档</el-button>
      </el-card>

      <!-- 记录区 -->
      <el-card style="margin-top: 16px">
        <el-tabs>
          <el-tab-pane :label="`应急记录(${d.measures.length})`">
            <el-table :data="d.measures" size="small">
              <el-table-column prop="operatorName" label="处置人" width="100" />
              <el-table-column prop="measureTypes" label="围护措施" min-width="160" />
              <el-table-column label="派发时间" width="150"><template #default="{ row }">{{ fmt(row.dispatchedAt) }}</template></el-table-column>
              <el-table-column label="到场时间" width="150"><template #default="{ row }">{{ fmt(row.arrivedAt) }}</template></el-table-column>
              <el-table-column prop="cost" label="费用(元)" width="100" />
              <el-table-column prop="note" label="备注" min-width="140" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`权属排查(${d.investigations.length})`">
            <el-table :data="d.investigations" size="small">
              <el-table-column prop="investigatorName" label="排查人" width="90" />
              <el-table-column prop="coverMarking" label="井盖标识" min-width="110" show-overflow-tooltip />
              <el-table-column prop="pipelineInfo" label="地下管线" min-width="110" show-overflow-tooltip />
              <el-table-column prop="constructionHistory" label="历史施工" min-width="110" show-overflow-tooltip />
              <el-table-column prop="merchantInfo" label="周边商户" min-width="100" show-overflow-tooltip />
              <el-table-column prop="urbanRecords" label="城管台账" min-width="100" show-overflow-tooltip />
              <el-table-column prop="candidateUnitName" label="候选单位" width="100" />
              <el-table-column label="结论" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.conclusion === 'CONFIRMED' ? 'success' : row.conclusion === 'UNDETERMINED' ? 'danger' : 'info'">
                    {{ { CONFIRMED: '确认归属', UNDETERMINED: '责任待定', CONTINUE: '继续排查' }[row.conclusion] }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`维修工单(${d.workOrders.length})`">
            <el-table :data="d.workOrders" size="small">
              <el-table-column prop="orderNo" label="工单号" width="160" />
              <el-table-column prop="repairTeamName" label="维修队" width="110" />
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="STATUS_TAG[row.status]">{{ store.label('workOrderStatus', row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="到场时间" width="150"><template #default="{ row }">{{ fmt(row.arrivedAt) }}</template></el-table-column>
              <el-table-column prop="chamberCondition" label="井室状况" min-width="110" show-overflow-tooltip />
              <el-table-column prop="coverSpec" label="井盖规格" width="150" show-overflow-tooltip />
              <el-table-column label="封路" width="60"><template #default="{ row }">{{ row.roadClosure ? '是' : '否' }}</template></el-table-column>
              <el-table-column prop="professionalUnits" label="专业协同" width="100">
                <template #default="{ row }">{{ row.professionalUnits || '-' }}</template>
              </el-table-column>
              <el-table-column prop="repairCost" label="费用(元)" width="90" />
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="isRepairTeam && row.status === 'ISSUED'" size="small" type="primary" @click="doAcceptOrder(row)">接收</el-button>
                  <el-button v-if="isRepairTeam && ['ACCEPTED', 'RETURNED'].includes(row.status)" size="small" type="primary" @click="openReport(row)">补充维修信息</el-button>
                  <el-button v-if="isRepairTeam && ['ACCEPTED', 'RETURNED'].includes(row.status)" size="small" type="success" @click="doFinish(row)">完工</el-button>
                  <el-button v-if="isInspector && row.status === 'FINISHED'" size="small" type="warning" @click="openAcceptance(row)">验收</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`验收记录(${d.acceptances.length})`">
            <el-table :data="d.acceptances" size="small">
              <el-table-column prop="inspectorName" label="巡查员" width="90" />
              <el-table-column label="照片" width="70"><template #default="{ row }">{{ row.photoOk ? '✓' : '✗' }}</template></el-table-column>
              <el-table-column label="定位" width="70"><template #default="{ row }">{{ row.locationOk ? '✓' : '✗' }}</template></el-table-column>
              <el-table-column label="平整度" width="80"><template #default="{ row }">{{ row.flatnessOk ? '✓' : '✗' }}</template></el-table-column>
              <el-table-column label="碾压测试" width="90"><template #default="{ row }">{{ row.rollingTestOk ? '✓' : '✗' }}</template></el-table-column>
              <el-table-column label="结论" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.result === 'PASS' ? 'success' : 'danger'">{{ row.result === 'PASS' ? '通过' : '不通过' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="意见" min-width="140" />
              <el-table-column label="时间" width="150"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`赔付案件(${d.compensations.length})`">
            <el-table :data="d.compensations" size="small">
              <el-table-column prop="caseNo" label="案件号" width="160" />
              <el-table-column prop="claimantName" label="索赔人" width="90" />
              <el-table-column prop="claimantType" label="类型" width="90" />
              <el-table-column prop="claimAmount" label="索赔(元)" width="90" />
              <el-table-column prop="decidedAmount" label="核定(元)" width="90" />
              <el-table-column prop="responsibility" label="责任判定" width="100" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="STATUS_TAG[row.status]">{{ store.label('compensationStatus', row.status) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`费用台账(${d.costs.length})`">
            <el-table :data="d.costs" size="small">
              <el-table-column prop="costType" label="费用类型" width="100" />
              <el-table-column prop="amount" label="金额(元)" width="100" />
              <el-table-column label="承担状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="STATUS_TAG[row.bearerStatus]">{{ store.label('costBearerStatus', row.bearerStatus) || (row.bearerStatus === 'PENDING' ? '责任待定' : '已分摊') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="承担方" width="130">
                <template #default="{ row }">
                  {{ row.bearerUnitName || { OWNER: '权属单位', STREET: '街道', SHARED: '多方分摊' }[row.bearerType] || '待定' }}
                </template>
              </el-table-column>
              <el-table-column prop="note" label="说明" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button v-if="canAllocate && row.bearerStatus === 'PENDING'" size="small" type="primary" @click="openAllocate(row)">复核分摊</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`时间线(${d.logs.length})`">
            <el-timeline style="padding-left: 6px">
              <el-timeline-item v-for="l in [...d.logs].reverse()" :key="l.id" :timestamp="fmt(l.createdAt)" placement="top">
                <b>{{ l.action }}</b> — {{ l.actorName }}
                <div class="log-detail">{{ l.detail }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <!-- 围护登记 -->
    <el-dialog v-model="dlg.secure" title="登记临时围护" width="480px">
      <el-form label-width="100px">
        <el-form-item label="围护措施" required>
          <el-checkbox-group v-model="secureForm.measureTypes">
            <el-checkbox v-for="t in store.enums?.measureTypes" :key="t" :value="t">{{ t }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="到场时间">
          <el-date-picker v-model="secureForm.arrivedAt" type="datetime" value-format="YYYY-MM-DD HH:mm" placeholder="默认当前时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="处置费用">
          <el-input-number v-model="secureForm.cost" :min="0" :precision="2" /> 元（记为责任待定）
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="secureForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.secure = false">取消</el-button>
        <el-button type="primary" @click="doSecure">确认围护完成</el-button>
      </template>
    </el-dialog>

    <!-- 权属排查 -->
    <el-dialog v-model="dlg.investigate" title="提交权属排查" width="620px">
      <el-form label-width="100px">
        <el-form-item label="井盖标识"><el-input v-model="invForm.coverMarking" placeholder="井圈/井盖铸字、标识牌等" /></el-form-item>
        <el-form-item label="地下管线"><el-input v-model="invForm.pipelineInfo" placeholder="管线资料比对结果" /></el-form-item>
        <el-form-item label="历史施工"><el-input v-model="invForm.constructionHistory" placeholder="历史施工/道路改造记录" /></el-form-item>
        <el-form-item label="周边商户"><el-input v-model="invForm.merchantInfo" placeholder="周边商户走访情况" /></el-form-item>
        <el-form-item label="城管台账"><el-input v-model="invForm.urbanRecords" placeholder="城管台账登记信息" /></el-form-item>
        <el-form-item label="候选单位">
          <el-select v-model="invForm.candidateUnitId" clearable style="width: 100%">
            <el-option v-for="u in units" :key="u.id" :label="u.name + '（' + u.type + '）'" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排查结论" required>
          <el-radio-group v-model="invForm.conclusion">
            <el-radio value="CONTINUE">继续排查</el-radio>
            <el-radio value="CONFIRMED">确认归属</el-radio>
            <el-radio value="UNDETERMINED">责任待定（街道先行处置）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="invForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.investigate = false">取消</el-button>
        <el-button type="primary" @click="doInvestigate">提交排查</el-button>
      </template>
    </el-dialog>

    <!-- 派发工单 -->
    <el-dialog v-model="dlg.order" title="派发维修工单" width="460px">
      <el-form label-width="90px">
        <el-form-item label="维修队" required>
          <el-select v-model="orderForm.repairTeamId" style="width: 100%">
            <el-option v-for="t in repairTeams" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="orderForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.order = false">取消</el-button>
        <el-button type="primary" @click="doCreateOrder">派单</el-button>
      </template>
    </el-dialog>

    <!-- 维修信息 -->
    <el-dialog v-model="dlg.report" title="补充维修信息" width="560px">
      <el-form label-width="100px">
        <el-form-item label="到场时间" required>
          <el-date-picker v-model="reportForm.arrivedAt" type="datetime" value-format="YYYY-MM-DD HH:mm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="井室状况" required>
          <el-input v-model="reportForm.chamberCondition" placeholder="如：井室完好/井壁破损/淤积" />
        </el-form-item>
        <el-form-item label="井盖规格" required>
          <el-input v-model="reportForm.coverSpec" placeholder="如：Φ700重型球墨铸铁井盖" />
        </el-form-item>
        <el-form-item label="需要封路">
          <el-switch v-model="reportForm.roadClosure" />
        </el-form-item>
        <el-form-item label="专业协同">
          <el-checkbox-group v-model="reportForm.professionalUnits">
            <el-checkbox v-for="t in store.enums?.professionalUnits" :key="t" :value="t">{{ t }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="维修说明"><el-input v-model="reportForm.repairDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="维修费用"><el-input-number v-model="reportForm.repairCost" :min="0" :precision="2" /> 元</el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.report = false">取消</el-button>
        <el-button type="primary" @click="doReport">保存</el-button>
      </template>
    </el-dialog>

    <!-- 验收 -->
    <el-dialog v-model="dlg.acceptance" title="巡查验收" width="480px">
      <el-form label-width="110px">
        <el-form-item label="照片核验"><el-switch v-model="accForm.photoOk" /></el-form-item>
        <el-form-item label="定位核验"><el-switch v-model="accForm.locationOk" /></el-form-item>
        <el-form-item label="路面平整度"><el-switch v-model="accForm.flatnessOk" /></el-form-item>
        <el-form-item label="车辆碾压测试"><el-switch v-model="accForm.rollingTestOk" /></el-form-item>
        <el-form-item label="验收结论" required>
          <el-radio-group v-model="accForm.result">
            <el-radio value="PASS">通过</el-radio>
            <el-radio value="FAIL">不通过（退回返工）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收意见"><el-input v-model="accForm.comment" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.acceptance = false">取消</el-button>
        <el-button type="primary" @click="doAcceptance">提交验收</el-button>
      </template>
    </el-dialog>

    <!-- 赔付立案 -->
    <el-dialog v-model="dlg.case" title="赔付立案" width="480px">
      <el-form label-width="90px">
        <el-form-item label="索赔人" required><el-input v-model="caseForm.claimantName" /></el-form-item>
        <el-form-item label="索赔类型" required>
          <el-select v-model="caseForm.claimantType" style="width: 100%">
            <el-option v-for="t in store.enums?.claimantTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="索赔金额" required><el-input-number v-model="caseForm.claimAmount" :min="0" :precision="2" /> 元</el-form-item>
        <el-form-item label="事故证明">
          <el-input v-model="caseForm.accidentProof" type="textarea" :rows="3" placeholder="事故认定书、医疗票据、维修发票等证明材料" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.case = false">取消</el-button>
        <el-button type="primary" @click="doCreateCase">立案</el-button>
      </template>
    </el-dialog>

    <!-- 费用分摊 -->
    <el-dialog v-model="dlg.allocate" title="费用复核分摊" width="460px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
                :title="`费用：${allocRow?.costType} ${allocRow?.amount} 元（当前责任待定）`" />
      <el-form label-width="90px">
        <el-form-item label="承担方" required>
          <el-radio-group v-model="allocForm.bearerType">
            <el-radio value="OWNER">权属单位</el-radio>
            <el-radio value="STREET">街道</el-radio>
            <el-radio value="SHARED">多方分摊</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="allocForm.bearerType === 'OWNER'" label="权属单位" required>
          <el-select v-model="allocForm.bearerUnitId" style="width: 100%">
            <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明"><el-input v-model="allocForm.note" type="textarea" :rows="2" placeholder="分摊依据，如复核确认的权属结果" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.allocate = false">取消</el-button>
        <el-button type="primary" @click="doAllocate">确认分摊</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { store, STATUS_TAG, DANGER_TAG } from '../store'

const route = useRoute()
const id = route.params.id

const loading = ref(false)
const d = ref({ event: null, measures: [], investigations: [], workOrders: [], acceptances: [], compensations: [], costs: [], logs: [] })
const units = ref([])
const repairTeams = ref([])

const dlg = reactive({ secure: false, investigate: false, order: false, report: false, acceptance: false, case: false, allocate: false })
const secureForm = reactive({ measureTypes: [], arrivedAt: '', cost: 0, note: '' })
const invForm = reactive({ coverMarking: '', pipelineInfo: '', constructionHistory: '', merchantInfo: '', urbanRecords: '', candidateUnitId: null, conclusion: 'CONTINUE', note: '' })
const orderForm = reactive({ repairTeamId: null, note: '' })
const reportForm = reactive({ arrivedAt: '', chamberCondition: '', coverSpec: '', roadClosure: false, professionalUnits: [], repairDesc: '', repairCost: 0 })
const accForm = reactive({ photoOk: false, locationOk: false, flatnessOk: false, rollingTestOk: false, result: 'PASS', comment: '' })
const caseForm = reactive({ claimantName: '', claimantType: '', claimAmount: 0, accidentProof: '' })
const allocForm = reactive({ bearerType: 'OWNER', bearerUnitId: null, note: '' })
const currentOrder = ref(null)
const allocRow = ref(null)

const role = computed(() => store.user?.role)
const isRepairTeam = computed(() => role.value === 'REPAIR_TEAM')
const isInspector = computed(() => role.value === 'INSPECTOR')
const canAllocate = computed(() => ['ADMIN', 'STREET_DUTY'].includes(role.value))

const measure = computed(() => d.value.measures[0])
const photoList = computed(() => (d.value.event?.photos || '').split(',').filter(Boolean))
const arrivalMinutes = computed(() => {
  const m = measure.value
  if (!m?.dispatchedAt || !m?.arrivedAt) return 0
  return Math.round((new Date(m.arrivedAt) - new Date(m.dispatchedAt)) / 60000)
})

const emergencyStep = computed(() => ({ PENDING: 1, DISPATCHED: 2, SECURED: 3 }[d.value.event?.emergencyStatus] ?? 1))
const ownerStep = computed(() => ({
  REPORTED: 0, INVESTIGATING: 1, OWNER_CONFIRMED: 2, OWNER_UNDETERMINED: 2,
  ORDER_ISSUED: 3, REPAIRING: 3, REPAIRED: 3, ACCEPTED: 4, CLOSED: 5
}[d.value.event?.status] ?? 0))

const hasOpenOrder = computed(() => d.value.workOrders.some(o => o.status !== 'VERIFIED'))
const hasOpenCase = computed(() => d.value.compensations.some(c => ['PENDING', 'DECIDED'].includes(c.status)))

const actions = computed(() => {
  const e = d.value.event
  if (!e) return []
  const list = []
  if (['STREET_DUTY', 'ADMIN'].includes(role.value)) {
    if (e.emergencyStatus === 'PENDING') list.push('dispatch')
    if (e.emergencyStatus === 'DISPATCHED') list.push('secure')
    if (e.ownershipStatus !== 'CONFIRMED' && !['ACCEPTED', 'CLOSED'].includes(e.status)) list.push('investigate')
    if (e.status === 'ACCEPTED') list.push('close')
  }
  if (role.value === 'OWNER_UNIT') {
    if (e.ownershipStatus !== 'CONFIRMED') list.push('claim')
    if (e.ownershipStatus === 'CONFIRMED' && e.ownerUnitId === store.user?.unitId && !hasOpenOrder.value && !['ACCEPTED', 'CLOSED'].includes(e.status)) list.push('order')
  }
  if (role.value === 'ADMIN' && e.ownershipStatus === 'CONFIRMED' && !hasOpenOrder.value && !['ACCEPTED', 'CLOSED'].includes(e.status)) list.push('order')
  if (['COMPENSATION', 'ADMIN'].includes(role.value) && e.damageCaused && !hasOpenCase.value) list.push('case')
  return list
})

function can(a) { return actions.value.includes(a) }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    d.value = await api.get(`/events/${id}`)
  } finally {
    loading.value = false
  }
}

async function doDispatch() {
  await api.post(`/events/${id}/dispatch`)
  ElMessage.success('已派发应急处置')
  load()
}

async function doSecure() {
  if (!secureForm.measureTypes.length) return ElMessage.warning('请选择围护措施')
  await api.post(`/events/${id}/secure`, { ...secureForm, measureTypes: secureForm.measureTypes.join(',') })
  ElMessage.success('临时围护已登记，费用记为责任待定')
  dlg.secure = false
  load()
}

async function doInvestigate() {
  await api.post(`/events/${id}/investigations`, invForm)
  ElMessage.success('排查记录已提交')
  dlg.investigate = false
  load()
}

async function doClaim() {
  await ElMessageBox.confirm('确认由本单位认领该井盖事件？', '权属认领', { type: 'warning' })
  await api.post(`/events/${id}/claim`)
  ElMessage.success('认领成功，请尽快派发维修工单')
  load()
}

async function doCreateOrder() {
  if (!orderForm.repairTeamId) return ElMessage.warning('请选择维修队')
  await api.post(`/events/${id}/work-orders`, orderForm)
  ElMessage.success('工单已派发')
  dlg.order = false
  load()
}

async function doAcceptOrder(row) {
  await api.post(`/work-orders/${row.id}/accept`)
  ElMessage.success('已接收工单')
  load()
}

function openReport(row) {
  currentOrder.value = row
  Object.assign(reportForm, {
    arrivedAt: row.arrivedAt ? row.arrivedAt.replace('T', ' ').slice(0, 16) : '',
    chamberCondition: row.chamberCondition || '', coverSpec: row.coverSpec || '',
    roadClosure: row.roadClosure || false,
    professionalUnits: (row.professionalUnits || '').split(',').filter(Boolean),
    repairDesc: row.repairDesc || '', repairCost: row.repairCost || 0
  })
  dlg.report = true
}

async function doReport() {
  await api.post(`/work-orders/${currentOrder.value.id}/report`, {
    ...reportForm, professionalUnits: reportForm.professionalUnits.join(',')
  })
  ElMessage.success('维修信息已保存')
  dlg.report = false
  load()
}

async function doFinish(row) {
  await ElMessageBox.confirm('确认维修完工？完工后将由城管巡查员验收。', '完工确认', { type: 'warning' })
  await api.post(`/work-orders/${row.id}/finish`)
  ElMessage.success('已完工，等待验收')
  load()
}

function openAcceptance(row) {
  currentOrder.value = row
  dlg.acceptance = true
}

async function doAcceptance() {
  await api.post(`/work-orders/${currentOrder.value.id}/acceptance`, accForm)
  ElMessage.success('验收结果已提交')
  dlg.acceptance = false
  load()
}

async function doCreateCase() {
  if (!caseForm.claimantName || !caseForm.claimantType || !caseForm.claimAmount) {
    return ElMessage.warning('请填写索赔人、类型和金额')
  }
  await api.post(`/events/${id}/compensations`, caseForm)
  ElMessage.success('赔付案件已立案')
  dlg.case = false
  load()
}

async function doClose() {
  await ElMessageBox.confirm('确认结案归档？', '结案', { type: 'warning' })
  await api.post(`/events/${id}/close`)
  ElMessage.success('已结案')
  load()
}

function openAllocate(row) {
  allocRow.value = row
  allocForm.bearerUnitId = d.value.event?.ownerUnitId || null
  dlg.allocate = true
}

async function doAllocate() {
  await api.post(`/costs/${allocRow.value.id}/allocate`, allocForm)
  ElMessage.success('费用已分摊')
  dlg.allocate = false
  load()
}

onMounted(async () => {
  load()
  units.value = await api.get('/meta/units')
  repairTeams.value = await api.get('/meta/repair-teams')
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.photos { margin-top: 12px; display: flex; gap: 10px; }
.line-desc { color: #909399; font-size: 12px; margin-left: 8px; }
.arrival { margin-top: 12px; text-align: center; color: #606266; }
.log-detail { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
