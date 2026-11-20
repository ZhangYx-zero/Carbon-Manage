import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '碳排总览' }
        },
        {
          path: 'energy',
          name: 'energy',
          component: () => import('@/views/EnergyView.vue'),
          meta: { title: '实时监测' }
        },
        {
          path: 'diagnosis',
          name: 'diagnosis',
          component: () => import('@/views/DiagnosisView.vue'),
          meta: { title: 'AI 诊断' }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrdersView.vue'),
          meta: { title: '整改工单' }
        },
        {
          path: 'enterprises',
          name: 'enterprises',
          component: () => import('@/views/EnterprisesView.vue'),
          meta: { title: '企业管理', roles: ['ADMIN'] }
        },
        {
          path: 'factors',
          name: 'factors',
          component: () => import('@/views/FactorsView.vue'),
          meta: { title: '碳排因子', roles: ['ADMIN'] }
        },
        {
          path: 'energy-types',
          name: 'energy-types',
          component: () => import('@/views/EnergyTypesView.vue'),
          meta: { title: '能源类型', roles: ['ADMIN'] }
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { title: '个人中心' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('cm_token')
  const role = (localStorage.getItem('cm_role') || '').toUpperCase()
  const home = role === 'OPERATOR' ? '/orders' : '/dashboard'
  if (!to.meta.public && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: home }
  }
  const allowed = to.meta.roles as string[] | undefined
  if (allowed && !allowed.includes(role)) {
    return { path: home }
  }
  return true
})

export default router
