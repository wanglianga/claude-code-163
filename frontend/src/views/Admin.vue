<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <b>权属单位</b>
          <el-button size="small" type="primary" @click="unitDlg = true">新增单位</el-button>
        </div>
      </template>
      <el-table :data="units" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="单位名称" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="contactPerson" label="联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="150" />
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <b>平台账号</b>
          <el-button size="small" type="primary" @click="userDlg = true">新增账号</el-button>
        </div>
      </template>
      <el-table :data="users" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ store.label('roles', row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属单位/部门" />
        <el-table-column prop="unitId" label="权属单位ID" width="100" />
      </el-table>
    </el-card>

    <el-dialog v-model="unitDlg" title="新增权属单位" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称" required><el-input v-model="unitForm.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="unitForm.type" placeholder="排水/供水/燃气/电力/通信" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="unitForm.contactPerson" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="unitForm.contactPhone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unitDlg = false">取消</el-button>
        <el-button type="primary" @click="createUnit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userDlg" title="新增账号" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="userForm.username" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="userForm.password" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="userForm.name" /></el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option v-for="r in store.enums?.roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单位"><el-input v-model="userForm.orgName" /></el-form-item>
        <el-form-item v-if="userForm.role === 'OWNER_UNIT'" label="权属单位">
          <el-select v-model="userForm.unitId" style="width: 100%">
            <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDlg = false">取消</el-button>
        <el-button type="primary" @click="createUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { store } from '../store'

const units = ref([])
const users = ref([])
const unitDlg = ref(false)
const userDlg = ref(false)
const unitForm = reactive({ name: '', type: '', contactPerson: '', contactPhone: '' })
const userForm = reactive({ username: '', password: '', name: '', role: 'CITIZEN', orgName: '', unitId: null })

async function load() {
  units.value = await api.get('/meta/units')
  users.value = await api.get('/meta/users')
}

async function createUnit() {
  if (!unitForm.name) return ElMessage.warning('请填写单位名称')
  await api.post('/meta/units', unitForm)
  ElMessage.success('已保存')
  unitDlg.value = false
  load()
}

async function createUser() {
  if (!userForm.username || !userForm.password || !userForm.name) return ElMessage.warning('请填写完整信息')
  await api.post('/meta/users', userForm)
  ElMessage.success('已保存')
  userDlg.value = false
  load()
}

onMounted(load)
</script>
