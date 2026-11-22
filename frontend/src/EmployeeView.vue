<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from './api.js'
import EmployeeEditor from './EmployeeEditor.vue'
import TransferHistory from './TransferHistory.vue'

const props = defineProps({ initialPage: { type: Object, default: null } })
const emit = defineEmits(['session-expired'])
const filters = reactive({ name: '', gender: '', dept: '' })
const departments = ref([])
let activeFilters = {}
let requestNumber = 0
const rows = ref(props.initialPage?.records ?? [])
const total = ref(props.initialPage?.total ?? 0)
const page = ref(1)
const size = ref(5)
const table = ref()
const selected = ref([])
const loading = ref(false)
const listError = ref('')
const editorOpen = ref(false)
const employee = ref(null)
const employeeHistory = ref(null)
const openingId = ref(null)
const saving = ref(false)
const deleting = ref(false)
const salaryFormat = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

onMounted(() => {
  loadDepartments()
  if (!props.initialPage) loadRows()
})

function showError(error) {
  if (error.status === 401) emit('session-expired')
  else ElMessage.error(error.message)
}

async function loadDepartments() {
  try {
    departments.value = await api('depts')
  } catch (error) {
    showError(error)
  }
}

async function loadRows() {
  const current = ++requestNumber
  loading.value = true
  listError.value = ''
  try {
    const data = await api('emps', { query: { ...activeFilters, page: page.value, size: size.value } })
    if (current !== requestNumber) return

    const lastPage = Math.max(1, Math.ceil(data.total / size.value))
    if (page.value > lastPage) {
      page.value = lastPage
      await loadRows()
      return
    }
    rows.value = data.records
    total.value = data.total
    table.value?.clearSelection()
  } catch (error) {
    if (current !== requestNumber) return
    if (error.status === 401) emit('session-expired')
    else listError.value = error.message
  } finally {
    if (current === requestNumber) loading.value = false
  }
}

function search() {
  activeFilters = Object.fromEntries(Object.entries(filters).filter(([, value]) => value))
  page.value = 1
  loadRows()
}

function reset() {
  Object.assign(filters, { name: '', gender: '', dept: '' })
  search()
}

function changeSize() {
  page.value = 1
  loadRows()
}

function openCreate() {
  employee.value = null
  editorOpen.value = true
}

async function openEdit(row) {
  if (openingId.value !== null) return
  openingId.value = row.empId
  try {
    employee.value = await api(`emps/${row.empId}`)
    editorOpen.value = true
  } catch (error) {
    showError(error)
  } finally {
    openingId.value = null
  }
}

async function save(body) {
  saving.value = true
  try {
    await api('emps', { method: employee.value ? 'PUT' : 'POST', body })
    editorOpen.value = false
    ElMessage.success('保存成功')
    await loadRows()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function deleteEmployees(row) {
  const ids = row ? [row.empId] : selected.value.map(employee => employee.empId)
  if (!ids.length || deleting.value) return

  deleting.value = true
  try {
    const message = row ? `确定删除员工「${row.empName}」吗？` : `确定删除选中的 ${ids.length} 名员工吗？`
    const confirmed = await ElMessageBox.confirm(message, '删除确认', { type: 'warning' })
      .catch(() => false)
    if (!confirmed) return

    await api(row ? `emps/${row.empId}` : 'emps', { method: 'DELETE', body: row ? undefined : ids })
    ElMessage.success('删除成功')
    await loadRows()
  } catch (error) {
    showError(error)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <h1 class="page-title">员工列表</h1>
  <el-card class="employee-panel" shadow="never">
    <el-form class="filters" :inline="true" @submit.prevent="search">
      <el-form-item label="姓名">
        <el-input v-model.trim="filters.name" placeholder="输入姓名" maxlength="50" clearable />
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="filters.gender" placeholder="全部" clearable>
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="filters.dept" class="department-select" placeholder="全部" filterable clearable>
          <el-option v-for="dept in departments" :key="dept.deptId"
                     :label="dept.deptName" :value="dept.deptName" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-tools">
      <el-button type="primary" :disabled="openingId !== null" @click="openCreate">新增员工</el-button>
      <el-button type="danger" plain :disabled="selected.length === 0" :loading="deleting"
                 @click="deleteEmployees(null)">批量删除</el-button>
    </div>

    <el-alert v-if="listError" :title="listError" type="error" show-icon :closable="false" class="form-alert" />
    <el-table ref="table" v-loading="loading" :data="rows" row-key="empId" border empty-text="暂无员工"
              @selection-change="selected = $event">
      <el-table-column type="selection" width="44" />
      <el-table-column prop="empId" label="编号" width="76" />
      <el-table-column prop="empName" label="姓名" min-width="110" show-overflow-tooltip />
      <el-table-column prop="gender" label="性别" width="65" />
      <el-table-column prop="dept" label="部门" min-width="108" show-overflow-tooltip />
      <el-table-column prop="post" label="岗位" min-width="120" show-overflow-tooltip />
      <el-table-column label="薪资" min-width="110" align="right">
        <template #default="{ row }">{{ row.salary == null ? '—' : salaryFormat.format(row.salary) }}</template>
      </el-table-column>
      <el-table-column prop="hireDate" label="入职日期" min-width="115" />
      <el-table-column label="状态" width="76">
        <template #default="{ row }">
          <el-tag v-if="row.status != null" :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '在职' : '离职' }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="178" fixed="right">
        <template #default="{ row }">
          <el-space :size="8">
            <el-button type="primary" link :loading="row.empId === openingId" @click="openEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="employeeHistory = row">调动记录</el-button>
            <el-button type="danger" link :disabled="deleting" @click="deleteEmployees(row)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :page-sizes="[5, 10, 20, 50]"
                     :total="total" layout="total, sizes, prev, pager, next" background
                     @current-change="loadRows" @size-change="changeSize" />
    </div>
  </el-card>

  <EmployeeEditor v-if="editorOpen" :employee="employee" :departments="departments" :saving="saving"
                  @cancel="editorOpen = false" @save="save" />
  <TransferHistory v-if="employeeHistory" :employee="employeeHistory"
                   @close="employeeHistory = null" @session-expired="emit('session-expired')" />
</template>
