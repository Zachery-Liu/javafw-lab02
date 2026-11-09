<script setup>
import { computed, reactive, ref } from 'vue'

const props = defineProps({ employee: { type: Object, default: null }, saving: Boolean })
const emit = defineEmits(['cancel', 'save'])
const form = ref()
const draft = reactive({
  empName: '', gender: '男', dept: '', post: '', salary: undefined, hireDate: null, status: 1,
  ...(props.employee ?? {}),
})
const title = computed(() => props.employee ? '编辑员工' : '新增员工')
const rules = {
  empName: [{ required: true, whitespace: true, message: '请输入员工姓名', trigger: 'blur' }],
  salary: [{ required: true, type: 'number', min: 0, message: '请输入有效薪资', trigger: 'change' }],
}

async function save() {
  if (props.saving || !await form.value.validate().catch(() => false)) return
  emit('save', {
    ...draft,
    empName: draft.empName.trim(),
    dept: draft.dept?.trim() || null,
    post: draft.post?.trim() || null,
    hireDate: draft.hireDate || null,
  })
}
</script>

<template>
  <el-dialog :model-value="true" :title="title" width="min(560px, calc(100vw - 32px))"
             :close-on-click-modal="false" :show-close="!saving" :close-on-press-escape="!saving"
             @close="emit('cancel')">
    <el-form ref="form" :model="draft" :rules="rules" label-width="88px" :disabled="saving"
             @submit.prevent="save">
      <el-form-item label="姓名" prop="empName">
        <el-input v-model="draft.empName" maxlength="50" />
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="draft.gender" placeholder="请选择">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-input v-model="draft.dept" maxlength="50" />
      </el-form-item>
      <el-form-item label="岗位">
        <el-input v-model="draft.post" maxlength="50" />
      </el-form-item>
      <el-form-item label="薪资" prop="salary">
        <el-input-number v-model="draft.salary" :min="0" :max="99999999.99" :precision="2"
                         :step="100" controls-position="right" />
      </el-form-item>
      <el-form-item label="入职日期">
        <el-date-picker v-model="draft.hireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="draft.status" placeholder="请选择">
          <el-option label="在职" :value="1" />
          <el-option label="离职" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>
