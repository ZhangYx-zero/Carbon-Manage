<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createFactor,
  deleteFactor,
  listFactors,
  updateFactor
} from '@/api/factor'
import { useEnergyTypeStore } from '@/store/energyType'

const energyTypeStore = useEnergyTypeStore()

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const today = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const emptyForm = () => {
  const first = energyTypeStore.enabledTypes[0]
  return {
    energyType: first?.typeCode || '',
    factorName: '',
    factorValue: 0.5703,
    unit: first?.unit || '',
    effectiveDate: today(),
    status: 1,
    remark: ''
  }
}

const form = reactive<any>(emptyForm())

/** 编辑停用类型的旧因子时，把该类型补进下拉，避免被迫改动历史因子。 */
const typeOptions = computed(() => {
  const options = [...energyTypeStore.enabledTypes]
  if (editingId.value) {
    const current = energyTypeStore.types.find((t) => t.typeCode === form.energyType)
    if (current && !options.some((t) => t.typeCode === current.typeCode)) {
      options.push(current)
    }
  }
  return options
})

const load = async () => {
  loading.value = true
  try {
    list.value = await listFactors()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

const openEdit = (row: any) => {
  editingId.value = row.id
  Object.assign(form, {
    energyType: row.energyType,
    factorName: row.factorName,
    factorValue: Number(row.factorValue),
    unit: row.unit,
    effectiveDate: row.effectiveDate,
    status: row.status,
    remark: row.remark
  })
  dialogVisible.value = true
}

const onTypeChange = (value: string) => {
  form.unit = energyTypeStore.unitOf(value)
}

const save = async () => {
  if (!form.factorName) {
    ElMessage.warning('请填写因子名称')
    return
  }
  if (!form.factorValue || form.factorValue <= 0) {
    ElMessage.warning('碳排因子必须大于 0')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateFactor(editingId.value, form)
    } else {
      await createFactor(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const remove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认删除因子「${row.factorName}」吗？已参与核算的因子不可删除，可改为停用。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteFactor(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(async () => {
  await energyTypeStore.ensureLoaded()
  Object.assign(form, emptyForm())
  await load()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h2>碳排因子</h2>
        <div class="sub">碳排因子决定「能耗用量 → 碳排放量」的换算关系</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增因子</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="能源类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.energyType === 'ELECTRICITY' ? 'warning' : 'info'" effect="plain">
              {{ energyTypeStore.labelOf(row.energyType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="factorName" label="因子名称" min-width="220" />
        <el-table-column label="因子值" width="140" align="right">
          <template #default="{ row }">{{ Number(row.factorValue).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="计量单位" width="110" />
        <el-table-column prop="effectiveDate" label="生效日期" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑碳排因子' : '新增碳排因子'" width="560px">
      <el-form label-width="100px">
        <el-form-item label="能源类型" required>
          <el-select v-model="form.energyType" style="width: 200px" placeholder="请选择能源类型" @change="onTypeChange">
            <el-option
              v-for="item in typeOptions"
              :key="item.typeCode"
              :label="item.status === 1 ? item.typeName : `${item.typeName}（已停用）`"
              :value="item.typeCode"
            />
          </el-select>
          <div v-if="energyTypeStore.enabledTypes.length === 0" style="width: 100%; font-size: 12px; color: #e6a23c">
            暂无启用的能源类型，请先在「能源类型」页面新增或启用类型
          </div>
        </el-form-item>
        <el-form-item label="因子名称" required>
          <el-input v-model="form.factorName" placeholder="如：华东电网电力排放因子" />
        </el-form-item>
        <el-form-item label="因子值" required>
          <el-input-number v-model="form.factorValue" :min="0.0001" :precision="4" :step="0.1" style="width: 200px" />
          <span style="margin-left: 8px; color: #86909c">kgCO2e / {{ form.unit }}</span>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.unit" style="width: 200px" disabled placeholder="由能源类型字典决定" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
