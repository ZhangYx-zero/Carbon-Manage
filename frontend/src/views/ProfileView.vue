<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMe } from '@/api/auth'
import { changePassword, createUser, listUsers, updateProfile } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const activeTab = ref('profile')

const PHONE_RE = /^1[3-9]\d{9}$/
const EMAIL_RE = /^[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}$/

const profile = ref<any>(null)
const loadingProfile = ref(false)

const profileForm = reactive({
  nickname: '',
  phone: '',
  email: ''
})
const savingProfile = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const savingPassword = ref(false)

const users = ref<any[]>([])
const loadingUsers = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  username: '',
  password: '123456',
  role: 'OPERATOR',
  nickname: '',
  phone: '',
  email: ''
})

const avatarText = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())

const roleLabel = (role: string) =>
  role === 'ADMIN' ? '管理员' : role === 'OPERATOR' ? '整改负责人' : role || '-'

const loadProfile = async () => {
  loadingProfile.value = true
  try {
    const data: any = await getMe()
    profile.value = data
    profileForm.nickname = data.nickname || ''
    profileForm.phone = data.phone || ''
    profileForm.email = data.email || ''
    if (data.nickname) {
      userStore.updateProfileInfo({ nickname: data.nickname })
    }
  } finally {
    loadingProfile.value = false
  }
}

const loadUsers = async () => {
  if (!isAdmin.value) return
  loadingUsers.value = true
  try {
    const data: any = await listUsers()
    users.value = data || []
  } finally {
    loadingUsers.value = false
  }
}

const saveProfile = async () => {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  if (profileForm.phone && !PHONE_RE.test(profileForm.phone.trim())) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (profileForm.email && !EMAIL_RE.test(profileForm.email.trim())) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  savingProfile.value = true
  try {
    const data: any = await updateProfile({
      nickname: profileForm.nickname.trim(),
      phone: profileForm.phone.trim() || undefined,
      email: profileForm.email.trim() || undefined
    })
    profile.value = data
    profileForm.nickname = data.nickname || ''
    profileForm.phone = data.phone || ''
    profileForm.email = data.email || ''
    userStore.updateProfileInfo({ nickname: data.nickname || '' })
    ElMessage.success('个人信息已更新')
  } finally {
    savingProfile.value = false
  }
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写原密码、新密码和确认密码')
    return
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 32) {
    ElMessage.warning('新密码长度需为 6~32 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword === passwordForm.oldPassword) {
    ElMessage.warning('新密码不能与原密码相同')
    return
  }
  savingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    userStore.logout()
    router.push('/login')
  } finally {
    savingPassword.value = false
  }
}

const openCreate = () => {
  Object.assign(createForm, {
    username: '',
    password: '123456',
    role: 'OPERATOR',
    nickname: '',
    phone: '',
    email: ''
  })
  createVisible.value = true
}

const submitCreate = async () => {
  const username = createForm.username.trim()
  if (!/^[A-Za-z0-9_]{3,20}$/.test(username)) {
    ElMessage.warning('登录账号需为 3~20 位字母、数字或下划线')
    return
  }
  if (createForm.password.length < 6 || createForm.password.length > 32) {
    ElMessage.warning('初始密码长度需为 6~32 位')
    return
  }
  if (createForm.phone && !PHONE_RE.test(createForm.phone.trim())) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (createForm.email && !EMAIL_RE.test(createForm.email.trim())) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  creating.value = true
  try {
    await createUser({
      username,
      password: createForm.password,
      role: createForm.role,
      nickname: createForm.nickname.trim() || undefined,
      phone: createForm.phone.trim() || undefined,
      email: createForm.email.trim() || undefined
    })
    ElMessage.success(`账号 ${username} 创建成功`)
    createVisible.value = false
    await loadUsers()
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  loadProfile()
  if (isAdmin.value) {
    loadUsers()
  }
})
</script>

<template>
  <div class="profile-page" v-loading="loadingProfile">
    <div class="page-header">
      <div>
        <h2>个人中心</h2>
        <div class="sub">维护本人资料与登录密码；管理员可在此新增整改负责人或管理员账号</div>
      </div>
    </div>

    <div class="profile-hero">
      <div class="avatar">{{ avatarText }}</div>
      <div class="hero-main">
        <div class="hero-name">
          {{ userStore.displayName }}
          <span class="hero-role">{{ userStore.roleLabel }}</span>
        </div>
        <div class="hero-meta">登录账号：{{ profile?.username || userStore.username }}</div>
        <div class="hero-meta">账号创建时间：{{ profile?.createTime || '-' }}</div>
      </div>
    </div>

    <div class="tab-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人信息" name="profile">
          <div class="form-wrap">
            <el-form label-width="90px" @submit.prevent>
              <el-form-item label="登录账号">
                <el-input :model-value="profile?.username" disabled />
              </el-form-item>
              <el-form-item label="账号角色">
                <el-input :model-value="roleLabel(profile?.role || userStore.role)" disabled />
              </el-form-item>
              <el-form-item label="昵称" required>
                <el-input v-model="profileForm.nickname" maxlength="50" placeholder="用于页面展示与工单负责人显示" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" maxlength="20" placeholder="选填，11 位手机号" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" maxlength="100" placeholder="选填，用于接收通知" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
              </el-form-item>
            </el-form>
            <div class="form-tip">登录账号与角色由管理员维护，个人中心只能修改昵称、手机号和邮箱。</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <div class="form-wrap">
            <el-alert
              title="密码使用 BCrypt 哈希后写入数据库，系统不保存明文"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 18px"
            />
            <el-form label-width="90px" @submit.prevent>
              <el-form-item label="原密码" required>
                <el-input v-model="passwordForm.oldPassword" type="password" show-password maxlength="32" />
              </el-form-item>
              <el-form-item label="新密码" required>
                <el-input v-model="passwordForm.newPassword" type="password" show-password maxlength="32" />
              </el-form-item>
              <el-form-item label="确认密码" required>
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password maxlength="32" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingPassword" @click="submitPassword">
                  确认修改密码
                </el-button>
              </el-form-item>
            </el-form>
            <div class="form-tip">新密码需为 6~32 位；修改成功后当前登录状态会被清除，需要使用新密码重新登录。</div>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="isAdmin" label="账号管理" name="accounts">
          <div class="accounts-head">
            <div class="form-tip" style="margin: 0">
              管理员可新增整改负责人，也可以再注册一名管理员账号。新账号的初始密码同样以 BCrypt 加密存储。
            </div>
            <el-button type="primary" :icon="Plus" @click="openCreate">新增账号</el-button>
          </div>
          <el-table v-loading="loadingUsers" :data="users" stripe style="margin-top: 14px">
            <el-table-column prop="username" label="登录账号" width="150" />
            <el-table-column prop="nickname" label="昵称" width="140">
              <template #default="{ row }">{{ row.nickname || '-' }}</template>
            </el-table-column>
            <el-table-column label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'success' : 'primary'">
                  {{ roleLabel(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" width="140">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="180">
              <template #default="{ row }">{{ row.email || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="createVisible" title="新增账号" width="540px">
      <el-form label-width="90px">
        <el-form-item label="登录账号" required>
          <el-input v-model="createForm.username" maxlength="20" placeholder="3~20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="账号角色" required>
          <el-radio-group v-model="createForm.role">
            <el-radio value="OPERATOR">整改负责人</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" maxlength="50" placeholder="不填则默认使用登录账号" />
        </el-form-item>
        <el-form-item label="初始密码" required>
          <el-input v-model="createForm.password" type="password" show-password maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" maxlength="20" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" maxlength="100" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建账号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  margin-bottom: 16px;
  color: #eefaf4;
  background: linear-gradient(135deg, #0d3b2e 0%, #1d6f55 65%, #2f9e72 100%);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(13, 59, 46, 0.18);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  font-size: 28px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.18);
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-radius: 50%;
}

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
}

.hero-role {
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
}

.hero-meta {
  font-size: 13px;
  color: rgba(238, 250, 244, 0.78);
}

.tab-card {
  padding: 8px 20px 24px;
  background: #fff;
  border-radius: 12px;
}

.form-wrap {
  max-width: 540px;
  padding-top: 8px;
}

.form-tip {
  font-size: 12px;
  line-height: 1.7;
  color: #86909c;
}

.accounts-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 8px;
}
</style>
