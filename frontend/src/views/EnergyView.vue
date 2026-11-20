<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { collectEnergy, pageEnergy } from '@/api/energy'
import { triggerDiagnosis } from '@/api/diagnosis'
import { listEnabledEnterprises } from '@/api/enterprise'
import { useEnergyTypeStore } from '@/store/energyType'

const energyTypeStore = useEnergyTypeStore()

const loading = ref(false)
const collecting = ref(false)
const autoRefresh = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const enterprises = ref<any[]>([])
let timer: number | undefined

const filters = reactive({
  enterpriseId: undefined as number | undefined,
  energyType: '',
  exceeded: '' as '' | 'true' | 'false',
  pageNum: 1,
  pageSize: 10
})

const buildParams = () => ({
  pageNum: filters.pageNum,
  pageSize: filters.pageSize,
  enterpriseId: filters.enterpriseId,
  energyType: filters.energyType || undefined,
  exceeded: filters.exceeded === '' ? undefined : filters.exceeded
})

const load = async () => {
  loading.value = true
  try {
    const data: any = await pageEnergy(buildParams())
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

const handleCollect = async () => {
  collecting.value = true
  try {
    const count: any = await collectEnergy()
    ElMessage.success(`模拟采集完成，新增 ${count} 条能耗数据`)
    await load()
  } finally {
    collecting.value = false
  }
}

const handleDiagnose = async (row: any) => {
  if (row.isExceeded !== 1) {
    ElMessage.warning('该条数据未超标，无需 AI 诊断')
    return
  }
  const logId: any = await triggerDiagnosis(row.id)
  if (logId) {
    ElMessage.success('已提交 AI 诊断，请到「AI 诊断」页查看结果')
  } else {
    ElMessage.warning('该条数据已诊断过，无需重复触发')
  }
}

onMounted(async () => {
  await energyTypeStore.ensureLoaded()
  const data: any = await listEnabledEnterprises()
  enterprises.value = data
  await load()
  timer = window.setInterval(() => {
    if (autoRefresh.value) {
      load()
    }
  }, 10000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h2>实时监测</h2>
        <div class="sub">IoT 模拟采集的能耗与碳排时序数据，每 10 秒自动刷新</div>
      </div>
      <div>
        <el-switch v-model="autoRefresh" active-text="自动刷新" style="margin-right: 12px" />
        <el-button type="primary" :loading="collecting" @click="handleCollect">
          手动采集
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.enterpriseId" placeholder="全部企业" clearable style="width: 220px" @change="search">
        <el-option v-for="item in enterprises" :key="item.id" :label="item.enterpriseName" :value="item.id" />
      </el-select>
      <el-select v-model="filters.energyType" placeholder="全部能源" clearable style="width: 150px" @change="search">
        <el-option v-for="item in energyTypeStore.types" :key="item.typeCode" :label="item.typeName" :value="item.typeCode" />
      </el-select>
      <el-select v-model="filters.exceeded" placeholder="全部状态" clearable style="width: 150px" @change="search">
        <el-option label="已超标" value="true" />
        <el-option label="正常" value="false" />
      </el-select>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="collectTime" label="采集时间" width="170" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="200" />
        <el-table-column prop="workshopName" label="车间" width="140" />
        <el-table-column label="能源类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.energyType === 'ELECTRICITY' ? 'warning' : 'info'" effect="plain">
              {{ energyTypeStore.labelOf(row.energyType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="能耗用量" width="140" align="right">
          <template #default="{ row }">
            {{ Number(row.energyValue).toFixed(2) }} {{ energyTypeStore.unitOf(row.energyType) }}
          </template>
        </el-table-column>
        <el-table-column label="碳排放量 (kgCO2e)" width="160" align="right">
          <template #default="{ row }">
            {{ Number(row.emissionValue).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="阈值 (kgCO2e)" width="140" align="right">
          <template #default="{ row }">
            {{ Number(row.thresholdValue).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isExceeded === 1" type="danger" effect="dark">已超标</el-tag>
            <el-tag v-else type="success" effect="plain">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 诊断" width="110" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.isExceeded === 1"
              link
              type="primary"
              @click="handleDiagnose(row)"
            >
              AI 诊断
            </el-button>
            <span v-else class="not-needed">无需诊断</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-needed {
  font-size: 13px;
  color: #c0c4cc;
}
</style>
