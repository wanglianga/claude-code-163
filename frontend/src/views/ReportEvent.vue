<template>
  <el-card style="max-width: 760px">
    <template #header><b>市民上报井盖问题</b></template>
    <el-form :model="form" label-width="110px">
      <el-form-item label="位置" required>
        <el-input v-model="form.location" placeholder="如：人民路与建设大街交口东南角" />
      </el-form-item>
      <el-form-item label="路段名称">
        <el-input v-model="form.roadName" placeholder="如：人民路（用于高发路段统计）" />
      </el-form-item>
      <el-form-item label="道路类型" required>
        <el-select v-model="form.roadType" style="width: 100%">
          <el-option v-for="t in store.enums?.roadTypes" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="经纬度">
        <el-input-number v-model="form.lng" :precision="6" :step="0.0001" placeholder="经度" style="width: 48%" />
        <el-input-number v-model="form.lat" :precision="6" :step="0.0001" placeholder="纬度" style="width: 48%; margin-left: 4%" />
      </el-form-item>
      <el-form-item label="问题类型" required>
        <el-radio-group v-model="form.problemType">
          <el-radio v-for="t in store.enums?.problemTypes" :key="t" :value="t">{{ t }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="危险程度" required>
        <el-radio-group v-model="form.dangerLevel">
          <el-radio-button value="高">高</el-radio-button>
          <el-radio-button value="中">中</el-radio-button>
          <el-radio-button value="低">低</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="现场照片">
        <el-upload :http-request="upload" list-type="picture-card" :on-remove="onRemove" accept="image/*">
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="已造成损伤">
        <el-switch v-model="form.damageCaused" active-text="已造成人车损伤" />
      </el-form-item>
      <el-form-item v-if="form.damageCaused" label="损伤情况">
        <el-input v-model="form.damageDesc" type="textarea" :rows="2"
                  placeholder="请描述人员受伤或车辆/财产受损情况" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">提交上报</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { store } from '../store'

const router = useRouter()
const saving = ref(false)
const photos = ref([])

const form = reactive({
  location: '', roadName: '', roadType: '', lng: null, lat: null,
  problemType: '', dangerLevel: '', damageCaused: false, damageDesc: ''
})

async function upload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  const data = await api.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  photos.value.push(data.url)
}

function onRemove(file, fileList) {
  // 简化：按索引移除
  photos.value = photos.value.slice(0, Math.max(0, photos.value.length - 1))
}

async function submit() {
  if (!form.location || !form.roadType || !form.problemType || !form.dangerLevel) {
    return ElMessage.warning('请填写位置、道路类型、问题类型和危险程度')
  }
  saving.value = true
  try {
    await api.post('/events', { ...form, photos: photos.value.join(',') })
    ElMessage.success('上报成功，平台已推送街道值班员处置')
    router.push('/events')
  } finally {
    saving.value = false
  }
}
</script>
