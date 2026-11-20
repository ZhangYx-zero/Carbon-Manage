<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDiagnosis, pageDiagnosis, retryDiagnosis } from '@/api/diagnosis'
import { useEnergyTypeStore } from '@/store/energyType'

const energyTypeStore = useEnergyTypeStore()
const STATUS_META: Record<string, { label: string; type: string }> = {
  RUNNING: { label: '诊断中', type: 'info' },
  SUCCESS: { label: '诊断成功', type: 'success' },
  FALLBACK: { label: '规则兜底', type: 'warning' },
  FAILED: { label: '失败', type: 'danger' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const current = ref<any>(null)
const filters = reactive({
  status: '',
  pageNum: 1,
  pageSize: 10
})

const load = async () => {
  loading.value = true
  try {
    const data: any = await pageDiagnosis({
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      status: filters.status || undefined
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  filters.pageNum = 1
  load()
}

const viewDetail = async (row: any) => {
  current.value = await getDiagnosis(row.id)
  dialogVisible.value = true
}

const handleRetry = async (row: any) => {
  await retryDiagnosis(row.id)
  ElMessage.success('已重新提交诊断')
  await load()
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
        <h2>AI 智能诊断</h2>
        <div class="sub">本地 Ollama 大模型对超标数据输出异常分析与减排整改建议</div>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 180px" @change="search">
        <el-option label="诊断中" value="RUNNING" />
        <el-option label="诊断成功" value="SUCCESS" />
        <el-option label="规则兜底" value="FALLBACK" />
        <el-option label="失败" value="FAILED" />
      </el-select>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="190" />
        <el-table-column prop="workshopName" label="车间" width="130" />
        <el-table-column label="能源类型" width="100">
          <template #default="{ row }">
            {{ energyTypeStore.labelOf(row.energyType) }}
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="诊断模型" width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="(STATUS_META[row.status]?.type as any) || 'info'">
              {{ STATUS_META[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            {{ row.durationMs ? `${(row.durationMs / 1000).toFixed(1)}s` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="关联工单" width="100" align="center">
          <template #default="{ row }">{{ row.orderId || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看详情</el-button>
            <el-button v-if="row.status === 'FAILED'" link type="warning" @click="handleRetry(row)">
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="AI 诊断详情" width="760px">
      <template v-if="current">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="企业">{{ current.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="车间">{{ current.workshopName }}</el-descriptions-item>
          <el-descriptions-item label="能源类型">{{ energyTypeStore.labelOf(current.energyType) }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ current.modelName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="(STATUS_META[current.status]?.type as any) || 'info'">
              {{ STATUS_META[current.status]?.label || current.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">
            {{ current.durationMs ? `${(current.durationMs / 1000).toFixed(1)}s` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="关联工单">{{ current.orderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ current.createTime }}</el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="current.errorMsg" type="warning" :closable="false" show-icon
                  :title="`错误信息：${current.errorMsg}`" style="margin-bottom: 16px" />

        <h4>Prompt 快照</h4>
        <div class="suggestion-text" style="background: #f7f9fb; padding: 12px; border-radius: 8px">
          {{ current.promptSnapshot }}
        </div>

        <h4>大模型诊断结果</h4>
        <div class="suggestion-text">{{ current.diagnosis || '诊断尚未完成' }}</div>
      </template>
    </el-dialog>
  </div>
</template>
