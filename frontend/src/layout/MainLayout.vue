<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useEnergyTypeStore } from '@/store/energyType'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const energyTypeStore = useEnergyTypeStore()

// 进入布局后预热能源类型字典，供各页面的名称/单位映射与下拉使用
energyTypeStore.ensureLoaded()

const onCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <el-container class="app-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><Sunny /></el-icon>
        <span>碳资产管理</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="menu"
        background-color="#0d3b2e"
        text-color="#b7d4c8"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>碳排总览</span>
        </el-menu-item>
        <el-menu-item index="/energy">
          <el-icon><Odometer /></el-icon>
          <span>实时监测</span>
        </el-menu-item>
        <el-menu-item index="/diagnosis">
          <el-icon><MagicStick /></el-icon>
          <span>AI 诊断</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>整改工单</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/enterprises">
          <el-icon><OfficeBuilding /></el-icon>
          <span>企业管理</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/factors">
          <el-icon><Coin /></el-icon>
          <span>碳排因子</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/energy-types">
          <el-icon><Collection /></el-icon>
          <span>能源类型</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">基于 SpringBoot 的企业碳资产智能监测系统</div>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-icon><UserFilled /></el-icon>
            {{ userStore.displayName }}
            <span v-if="userStore.roleLabel" class="role-tag">{{ userStore.roleLabel }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-container {
  height: 100%;
}

.aside {
  display: flex;
  flex-direction: column;
  background: #0d3b2e;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 20px;
  font-size: 17px;
  font-weight: 700;
  color: #e8f5ef;
  letter-spacing: 1px;
}

.menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #123c2e;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #1f2d3d;
  cursor: pointer;
}

.arrow {
  font-size: 12px;
  color: #86909c;
}

.role-tag {
  padding: 1px 8px;
  border-radius: 10px;
  background: #e8f5ef;
  color: #0d3b2e;
  font-size: 12px;
}

.main {
  padding: 20px;
  background: #f3f6f9;
}
</style>
