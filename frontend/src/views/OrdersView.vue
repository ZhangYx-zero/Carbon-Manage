<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { dispatchOrder, pageOrders, passOrder, rejectOrder, submitOrder } from '@/api/order'
import { listOperators } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isOperator = computed(() => userStore.isOperator)

const STATUS_META: Record<string, { label: string; type: string }> = {
  PENDING_DISPATCH: { label: '待派发', type: 'warning' },
  RECTIFYING: { label: '整改中', type: 'primary' },
  PENDING_AUDIT: { label: '待审核', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const filters = reactive({ status: '', pageNum: 1, pageSize: 10 })

const drawerVisible = ref(false)
const current = ref<any>(null)

const actionVisible = ref(false)
const actionSubmitting = ref(false)
const actionType = ref<'dispatch' | 'submit' | 'audit'>('dispatch')
const actionRow = ref<any>(null)
const operators = ref<any[]>([])
const actionForm = reactive({
  assigneeId: undefined as number | undefined,
  deadline: '',
  rectifyResult: '',
  auditRemark: '',
  auditResult: 'PASS' as 'PASS' | 'REJECT'
})

const actionTitle = computed(() => {
  if (actionType.value === 'dispatch') return '派发工单'
  if (actionType.value === 'submit') return '提交整改结果'
  return '审核工单'
})

const auditRemarkLabel = computed(() =>
  actionForm.auditResult === 'REJECT' ? '驳回原因' : '审核意见'
)

const pad = (n: number) => String(n).padStart(2, '0')
const formatDateTime = (date: Date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`

const load = async () => {
  loading.value = true
  try {
    const data: any = await pageOrders({
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

const loadOperators = async () => {
  try {
    const data: any = await listOperators()
    operators.value = data || []
  } catch {
    operators.value = []
  }
}

const search = () => {
  filters.pageNum = 1
  load()
}

const openDetail = (row: any) => {
  current.value = row
  drawerVisible.value = true
}

const openAction = (row: any, type: 'dispatch' | 'submit' | 'audit') => {
  actionType.value = type
  actionRow.value = row
  actionForm.assigneeId = undefined
  actionForm.deadline = formatDateTime(new Date(Date.now() + 3 * 86400000))
  actionForm.rectifyResult = type === 'submit' ? row.rectifyResult || '' : ''
  actionForm.auditRemark = ''
  actionForm.auditResult = 'PASS'
  if (type === 'dispatch' && operators.value.length === 0) {
    loadOperators()
  }
  actionVisible.value = true
}

const submitAction = async () => {
  if (!actionRow.value) return
  const id = actionRow.value.id
  if (actionType.value === 'dispatch' && !actionForm.assigneeId) {
    ElMessage.warning('请选择整改负责人')
    return
  }
  if (actionType.value === 'submit' && !actionForm.rectifyResult.trim()) {
    ElMessage.warning('请填写整改结果')
    return
  }
  if (actionType.value === 'audit' && !actionForm.auditRemark.trim()) {
    ElMessage.warning(`请填写${auditRemarkLabel.value}`)
    return
  }
  actionSubmitting.value = true
  try {
    if (actionType.value === 'dispatch') {
      await dispatchOrder(id, {
        assigneeId: actionForm.assigneeId,
        deadline: actionForm.deadline || undefined
      })
    } else if (actionType.value === 'submit') {
      await submitOrder(id, { rectifyResult: actionForm.rectifyResult })
    } else if (actionForm.auditResult === 'PASS') {
      await passOrder(id, { auditRemark: actionForm.auditRemark })
    } else {
      await rejectOrder(id, { auditRemark: actionForm.auditRemark })
    }
    ElMessage.success('操作成功')
    actionVisible.value = false
    await load()
  } finally {
    actionSubmitting.value = false
  }
}

onMounted(() => {
  load()
  if (isAdmin.value) {
    loadOperators()
  }
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h2>减排整改工单</h2>
        <div class="sub">
          {{
            isOperator
              ? '我的整改工单：仅显示派发给本人的工单'
              : '状态机流转：待派发 → 整改中 → 待审核 → 已完成，审核不通过退回整改中'
          }}
        </div>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="filters.status" @change="search">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button v-if="isAdmin" label="PENDING_DISPATCH">待派发</el-radio-button>
        <el-radio-button label="RECTIFYING">整改中</el-radio-button>
        <el-radio-button label="PENDING_AUDIT">待审核</el-radio-button>
        <el-radio-button label="COMPLETED">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="工单号" width="200" />
        <el-table-column prop="title" label="工单标题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" label="企业" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="(STATUS_META[row.status]?.type as any) || 'info'">
              {{ STATUS_META[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="负责人" width="110">
          <template #default="{ row }">{{ row.assignee || '-' }}</template>
        </el-table-column>
        <el-table-column prop="deadline" label="整改截止" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="isAdmin && row.status === 'PENDING_DISPATCH'"
              link
              type="warning"
              @click="openAction(row, 'dispatch')"
            >
              派单
            </el-button>
            <el-button
              v-if="isOperator && row.status === 'RECTIFYING'"
              link
              type="primary"
              @click="openAction(row, 'submit')"
            >
              提交整改
            </el-button>
            <el-button
              v-if="isAdmin && row.status === 'PENDING_AUDIT'"
              link
              type="success"
              @click="openAction(row, 'audit')"
            >
              审核
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

    <el-drawer v-model="drawerVisible" title="工单详情" size="520px">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="工单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
          <el-descriptions-item label="企业">{{ current.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="(STATUS_META[current.status]?.type as any) || 'info'">
              {{ STATUS_META[current.status]?.label || current.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ current.assignee || '-' }}</el-descriptions-item>
          <el-descriptions-item label="整改截止">{{ current.deadline || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ current.createTime }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="current.auditRemark && current.status !== 'COMPLETED'"
          title="驳回原因"
          type="warning"
          :closable="false"
          :description="current.auditRemark"
          style="margin-top: 14px"
        />

        <h4>AI 减排建议</h4>
        <div class="suggestion-text" style="background: #f7f9fb; padding: 12px; border-radius: 8px">
          {{ current.suggestion }}
        </div>

        <template v-if="current.rectifyResult">
          <h4>整改结果</h4>
          <div class="suggestion-text">{{ current.rectifyResult }}</div>
        </template>
        <template v-if="current.auditRemark && current.status === 'COMPLETED'">
          <h4>审核意见</h4>
          <div class="suggestion-text">{{ current.auditRemark }}</div>
        </template>
      </template>
    </el-drawer>

    <el-dialog v-model="actionVisible" :title="actionTitle" width="480px">
      <el-form label-width="90px">
        <template v-if="actionType === 'dispatch'">
          <el-form-item label="负责人" required>
            <el-select v-model="actionForm.assigneeId" placeholder="请选择整改负责人" style="width: 100%">
              <el-option
                v-for="op in operators"
                :key="op.id"
                :label="`${op.nickname || op.username}（${op.username}）`"
                :value="op.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker
              v-model="actionForm.deadline"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="默认 3 天后"
              style="width: 100%"
            />
          </el-form-item>
        </template>
        <template v-else-if="actionType === 'submit'">
          <el-form-item label="整改结果" required>
            <el-input
              v-model="actionForm.rectifyResult"
              type="textarea"
              :rows="4"
              placeholder="请填写整改措施与效果"
            />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="审核结果" required>
            <el-radio-group v-model="actionForm.auditResult">
              <el-radio value="PASS">通过</el-radio>
              <el-radio value="REJECT">不通过（退回整改）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="auditRemarkLabel" required>
            <el-input
              v-model="actionForm.auditRemark"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              :placeholder="`请填写${auditRemarkLabel}`"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionSubmitting" @click="submitAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
