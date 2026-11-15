<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from './api.js'

const emit = defineEmits(['session-expired'])
const keyword = ref('')
let activeKeyword = ''
const rows = ref([])
const loading = ref(false)
const listError = ref('')
const editorOpen = ref(false)
const editor = ref()
const draft = reactive({ deptId: null, deptName: '' })
const saving = ref(false)
const rules = {
  deptName: [{ required: true, whitespace: true, message: '请输入部门名称', trigger: 'blur' }],
}
const deletingId = ref(null)
const rosterOpen = ref(false)
const roster = reactive({ department: null, page: 1, rows: [], total: 0, loading: false, error: '' })
const rosterSize = 5
let rosterRequest = 0

onMounted(loadRows)

function showError(error) {
  if (error.status === 401) emit('session-expired')
  else ElMessage.error(error.message)
}

async function loadRows() {
  loading.value = true
  listError.value = ''
  try {
    rows.value = await api('depts', { query: activeKeyword ? { name: activeKeyword } : undefined })
  } catch (error) {
    if (error.status === 401) emit('session-expired')
    else listError.value = error.message
  } finally {
    loading.value = false
  }
}

function search() {
  activeKeyword = keyword.value
  loadRows()
}

function reset() {
  keyword.value = ''
  search()
}

function openEditor(row) {
  Object.assign(draft, { deptId: row?.deptId ?? null, deptName: row?.deptName ?? '' })
  editorOpen.value = true
}

async function save() {
  if (saving.value || !await editor.value.validate().catch(() => false)) return

  saving.value = true
  try {
    const body = { deptName: draft.deptName.trim() }
    if (draft.deptId) body.deptId = draft.deptId
    await api('depts', { method: draft.deptId ? 'PUT' : 'POST', body })
    editorOpen.value = false
    ElMessage.success('保存成功')
    await loadRows()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  if (deletingId.value !== null) return
  deletingId.value = row.deptId
  try {
    const confirmed = await ElMessageBox.confirm(
      '确定删除部门「' + row.deptName + '」吗？', '删除确认', { type: 'warning' },
    ).catch(() => false)
    if (!confirmed) return

    await api('depts/' + row.deptId, { method: 'DELETE' })
    ElMessage.success('删除成功')
    await loadRows()
  } catch (error) {
    showError(error)
  } finally {
    deletingId.value = null
  }
}

function openRoster(row) {
  Object.assign(roster, { department: row, page: 1, rows: [], total: 0, error: '' })
  rosterOpen.value = true
  loadRoster()
}

async function loadRoster() {
  const current = ++rosterRequest
  roster.loading = true
  roster.error = ''
  try {
    const { records, total } = await api('depts/' + roster.department.deptId + '/emps', {
      query: { page: roster.page, size: rosterSize },
    })
    if (current !== rosterRequest) return
    roster.rows = records
    roster.total = total
  } catch (error) {
    if (current !== rosterRequest) return
    if (error.status === 401) emit('session-expired')
    else roster.error = error.message
  } finally {
    if (current === rosterRequest) roster.loading = false
  }
}
</script>

<template>
  <h1 class="page-title">部门列表</h1>
  <el-card shadow="never">
    <el-form class="filters" :inline="true" @submit.prevent="search">
      <el-form-item label="部门名称">
        <el-input v-model.trim="keyword" placeholder="输入部门名称" maxlength="50" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :disabled="loading">查询</el-button>
        <el-button :disabled="loading" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-tools">
      <el-button type="primary" @click="openEditor()">新增部门</el-button>
    </div>

    <el-alert v-if="listError" :title="listError" type="error" show-icon :closable="false" class="form-alert" />
    <el-table v-loading="loading" :data="rows" row-key="deptId" border empty-text="暂无部门">
      <el-table-column prop="deptId" label="编号" width="85" />
      <el-table-column prop="deptName" label="部门名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="employeeCount" label="员工人数" width="100" align="right" />
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <el-button type="primary" link @click="openRoster(row)">查看员工</el-button>
          <el-button type="primary" link @click="openEditor(row)">编辑</el-button>
          <el-button type="danger" link :disabled="deletingId !== null" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-if="editorOpen" v-model="editorOpen" :title="draft.deptId ? '编辑部门' : '新增部门'"
             width="min(480px, calc(100vw - 32px))" :close-on-click-modal="false"
             :show-close="!saving" :close-on-press-escape="!saving">
    <el-form ref="editor" :model="draft" :rules="rules" label-width="88px" :disabled="saving"
             @submit.prevent="save">
      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="draft.deptName" maxlength="50" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="editorOpen = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-if="rosterOpen" v-model="rosterOpen" :title="roster.department.deptName + '员工名单'"
             width="min(660px, calc(100vw - 32px))">
    <el-alert v-if="roster.error" :title="roster.error" type="error" show-icon :closable="false" class="form-alert" />
    <el-table v-loading="roster.loading" :data="roster.rows" row-key="empId" border empty-text="该部门暂无员工">
      <el-table-column prop="empId" label="编号" width="76" />
      <el-table-column prop="empName" label="姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="post" label="岗位" min-width="130" show-overflow-tooltip />
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="roster.page" :page-size="rosterSize" :total="roster.total"
                     layout="total, prev, pager, next" background @current-change="loadRoster" />
    </div>
  </el-dialog>
</template>
