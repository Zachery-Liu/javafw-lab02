<script setup>
import { onMounted, ref } from 'vue'
import { api } from './api.js'

const props = defineProps({ employee: { type: Object, required: true } })
const emit = defineEmits(['close', 'session-expired'])
const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 5
const loading = ref(false)
const error = ref('')
let requestNumber = 0
const timeFormat = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium', timeStyle: 'medium', hour12: false,
})

onMounted(load)

async function load() {
  const current = ++requestNumber
  loading.value = true
  error.value = ''
  try {
    const data = await api('emps/' + props.employee.empId + '/transfers', {
      query: { page: page.value, size: pageSize },
    })
    if (current !== requestNumber) return
    records.value = data.records
    total.value = data.total
  } catch (failure) {
    if (current !== requestNumber) return
    if (failure.status === 401) emit('session-expired')
    else error.value = failure.message
  } finally {
    if (current === requestNumber) loading.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="true" :title="employee.empName + '的调动记录'"
             width="min(760px, calc(100vw - 32px))" @close="emit('close')">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="form-alert" />
    <el-table v-loading="loading" :data="records" row-key="transferId" border empty-text="暂无调动记录">
      <el-table-column label="调动时间" min-width="165">
        <template #default="{ row }">{{ timeFormat.format(new Date(row.transferredAt)) }}</template>
      </el-table-column>
      <el-table-column label="原部门" min-width="115" show-overflow-tooltip>
        <template #default="{ row }">{{ row.fromDept ?? '未分配' }}</template>
      </el-table-column>
      <el-table-column label="新部门" min-width="115" show-overflow-tooltip>
        <template #default="{ row }">{{ row.toDept ?? '未分配' }}</template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" min-width="100" show-overflow-tooltip />
    </el-table>
    <div v-if="total > pageSize" class="pagination">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
                     layout="total, prev, pager, next" background @current-change="load" />
    </div>
  </el-dialog>
</template>
