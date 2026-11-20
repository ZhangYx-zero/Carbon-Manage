<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createEnergyType,
  deleteEnergyType,
  listEnergyTypes,
  updateEnergyType
} from '@/api/energyType'
import { useEnergyTypeStore } from '@/store/energyType'

const energyTypeStore = useEnergyTypeStore()

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const CODE_PATTERN = /^[A-Z][A-Z0-9_]{1,19}$/

const emptyForm = () => ({
  typeCode: '',
  typeName: '',
  unit: '',
  ratioMin: 0.65,
  ratioMax: 0.85,
  sortOrder: 0,
  status: 1,
  remark: ''
})

const form = reactive<any>(emptyForm())

const load = async () => {
  loading.value = true
  try {
    list.value = await listEnergyTypes()
  } finally {
    loading.value = false
  }
}

const refreshAll = async () => {
  await energyTypeStore.ensureLoaded(true)
  await load()
}

const openCreate = () => {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

const openEdit = (row: any) => {
  editingId.value = row.id
  Object.assign(form, {
    typeCode: row.typeCode,
    typeName: row.typeName,
    unit: row.unit,
    ratioMin: Number(row.ratioMin),
    ratioMax: Number(row.ratioMax),
    sortOrder: Number(row.sortOrder),
    status: row.status,
    remark: row.remark
  })
  dialogVisible.value = true
}

/** 编码只允许大写字母、数字和下划线，避免用户输入中文或小写后再被后端拒绝。 */
const onCodeInput = (value: string) => {
  form.typeCode = (value || '').toUpperCase().replace(/[^A-Z0-9_]/g, '')
}

const save = async () => {
  if (!CODE_PATTERN.test(form.typeCode)) {
    ElMessage.warning('编码须以大写字母开头，仅含大写字母、数字、下划线，长度 2-20')
    return
  }
  if (!form.typeName) {
    ElMessage.warning('请填写能源类型名称')
    return
  }
  if (!form.unit) {
    ElMessage.warning('请填写计量单位')
    return
  }
  if (!form.ratioMin || !form.ratioMax || form.ratioMin <= 0 || form.ratioMax < form.ratioMin) {
    ElMessage.warning('比例区间须满足 0 < 下限 ≤ 上限')
    return
  }
  if (form.ratioMax > 3) {
    ElMessage.warning('比例上限不能大于 3')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateEnergyType(editingId.value, form)
    } else {
      await createEnergyType(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await refreshAll()
  } catch {
    // 错误提示由请求拦截器统一弹出
  } finally {
    saving.value = false
  }
}

const remove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认删除能源类型「${row.typeName}」吗？已被企业配置、碳排因子或能耗数据引用的类型不可删除，可改为停用。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteEnergyType(row.id)
    ElMessage.success('删除成功')
    await refreshAll()
  } catch {
    // 后端会返回具体引用提示，由请求拦截器统一弹出
  }
}

onMounted(async () => {
  await energyTypeStore.ensureLoaded()
  await load()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h2>能源类型</h2>
        <div class="sub">维护系统可采集的能源类型，新增后可在企业、碳排因子与监测页面中使用</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增类型</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="typeCode" label="编码" width="130">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.typeCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="类型名称" min-width="140" />
        <el-table-column prop="unit" label="计量单位" width="110" />
        <el-table-column label="模拟比例区间" width="150" align="center">
          <template #default="{ row }">
            {{ Number(row.ratioMin).toFixed(2) }} ~ {{ Number(row.ratioMax).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑能源类型' : '新增能源类型'" width="560px">
      <el-form label-width="110px">
        <el-form-item label="类型编码" required>
          <el-input
            v-model="form.typeCode"
            :disabled="!!editingId"
            placeholder="如：STEAM，创建后不可修改"
            style="width: 220px"
            @input="onCodeInput"
          />
        </el-form-item>
        <el-form-item label="类型名称" required>
          <el-input v-model="form.typeName" placeholder="如：蒸汽" style="width: 220px" />
        </el-form-item>
        <el-form-item label="计量单位" required>
          <el-input v-model="form.unit" placeholder="如：t" style="width: 220px" />
          <div style="width: 100%; font-size: 12px; color: #86909c">
            展示在能耗数据与碳排因子中；已有能耗数据时单位不可修改
          </div>
        </el-form-item>
        <el-form-item label="模拟比例区间" required>
          <el-input-number v-model="form.ratioMin" :min="0.01" :max="3" :precision="2" :step="0.05" style="width: 140px" />
          <span style="margin: 0 8px; color: #86909c">~</span>
          <el-input-number v-model="form.ratioMax" :min="0.01" :max="3" :precision="2" :step="0.05" style="width: 140px" />
          <div style="width: 100%; font-size: 12px; color: #86909c">
            模拟采集时，目标排放 = 企业阈值 × [下限, 上限] 内的随机比例，用于制造超标与正常样本；须满足 0 &lt; 下限 ≤ 上限 ≤ 3
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" style="width: 140px" />
          <span style="margin-left: 8px; color: #86909c">越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <div style="font-size: 12px; color: #e6a23c">
            提示：新增类型后需为该类型配置碳排因子，才会开始生成能耗数据
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
