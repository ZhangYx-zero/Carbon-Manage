<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createEnterprise,
  deleteEnterprise,
  listEnterprises,
  updateEnterprise
} from '@/api/enterprise'
import { useEnergyTypeStore } from '@/store/energyType'

const energyTypeStore = useEnergyTypeStore()

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  enterpriseName: '',
  workshopName: '',
  industry: '',
  thresholdValue: 500,
  contactPerson: '',
  contactPhone: '',
  energyTypes: energyTypeStore.enabledTypes.map((t) => t.typeCode),
  status: 1
})

const form = reactive<any>(emptyForm())

/** 复选框：启用类型 + 当前已配置的停用类型（编辑旧企业时不被强制清除）。 */
const checkboxOptions = computed(() => {
  const options = [...energyTypeStore.enabledTypes]
  for (const code of form.energyTypes || []) {
    const hit = energyTypeStore.types.find((t) => t.typeCode === code)
    if (hit && !options.some((o) => o.typeCode === code)) {
      options.push(hit)
    }
  }
  return options
})

const load = async () => {
  loading.value = true
  try {
    list.value = await listEnterprises(keyword.value || undefined)
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
    enterpriseName: row.enterpriseName,
    workshopName: row.workshopName,
    industry: row.industry,
    thresholdValue: Number(row.thresholdValue),
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    energyTypes: Array.isArray(row.energyTypes) && row.energyTypes.length
      ? [...row.energyTypes]
      : energyTypeStore.enabledTypes.map((t) => t.typeCode),
    status: row.status
  })
  dialogVisible.value = true
}

const save = async () => {
  if (!form.enterpriseName || !form.workshopName) {
    ElMessage.warning('请填写企业名称与车间名称')
    return
  }
  if (!form.thresholdValue || form.thresholdValue <= 0) {
    ElMessage.warning('碳排放阈值必须大于 0')
    return
  }
  if (!form.energyTypes || form.energyTypes.length === 0) {
    ElMessage.warning('请至少选择一种能源类型')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateEnterprise(editingId.value, form)
    } else {
      await createEnterprise(form)
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
      `确认删除企业「${row.enterpriseName}」吗？已有能耗数据的企业不可删除，可改为停用。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteEnterprise(row.id)
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
        <h2>企业管理</h2>
        <div class="sub">维护企业、车间、采集能源类型及单次采集碳排放阈值</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增企业</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索企业名称 / 车间"
        clearable
        style="width: 280px"
        :prefix-icon="Search"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="enterpriseName" label="企业名称" min-width="200" />
        <el-table-column prop="workshopName" label="车间" width="150" />
        <el-table-column prop="industry" label="所属行业" width="120" />
        <el-table-column label="能源类型" width="160">
          <template #default="{ row }">
            <el-tag
              v-for="type in row.energyTypes || []"
              :key="type"
              :type="type === 'ELECTRICITY' ? 'warning' : 'info'"
              effect="plain"
              size="small"
              style="margin-right: 4px"
            >
              {{ energyTypeStore.labelOf(type) }}
            </el-tag>
            <span v-if="!row.energyTypes || row.energyTypes.length === 0" style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="碳排阈值 (kgCO2e)" width="170" align="right">
          <template #default="{ row }">{{ Number(row.thresholdValue).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑企业' : '新增企业'" width="560px">
      <el-form label-width="110px">
        <el-form-item label="企业名称" required>
          <el-input v-model="form.enterpriseName" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="车间名称" required>
          <el-input v-model="form.workshopName" placeholder="请输入车间名称" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-input v-model="form.industry" placeholder="如：装备制造" />
        </el-form-item>
        <el-form-item label="能源类型" required>
          <el-checkbox-group v-model="form.energyTypes">
            <el-checkbox v-for="item in checkboxOptions" :key="item.typeCode" :value="item.typeCode">
              {{ item.status === 1 ? item.typeName : `${item.typeName}（已停用）` }}
            </el-checkbox>
          </el-checkbox-group>
          <div style="width: 100%; font-size: 12px; color: #86909c">
            只采集勾选的能源类型，未勾选的类型不会生成能耗数据；类型可在「能源类型」页面维护
          </div>
        </el-form-item>
        <el-form-item label="碳排阈值" required>
          <el-input-number v-model="form.thresholdValue" :min="0.01" :precision="2" :step="10" style="width: 200px" />
          <span style="margin-left: 8px; color: #86909c">kgCO2e / 次采集</span>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
