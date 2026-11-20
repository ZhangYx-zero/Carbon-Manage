import { defineStore } from 'pinia'

interface LoginPayload {
  token: string
  username: string
  nickname: string
  role: string
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('cm_token') || '',
    username: localStorage.getItem('cm_username') || '',
    nickname: localStorage.getItem('cm_nickname') || '',
    role: (localStorage.getItem('cm_role') || '').toUpperCase()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.nickname || state.username || '未登录',
    isAdmin: (state) => state.role === 'ADMIN',
    isOperator: (state) => state.role === 'OPERATOR',
    roleLabel: (state) =>
      state.role === 'ADMIN' ? '管理员' : state.role === 'OPERATOR' ? '整改负责人' : ''
  },
  actions: {
    setLogin(payload: LoginPayload) {
      const role = String(payload.role || '').toUpperCase()
      this.token = payload.token
      this.username = payload.username
      this.nickname = payload.nickname
      this.role = role
      localStorage.setItem('cm_token', payload.token)
      localStorage.setItem('cm_username', payload.username)
      localStorage.setItem('cm_nickname', payload.nickname || '')
      localStorage.setItem('cm_role', role)
    },
    updateProfileInfo(payload: { nickname: string }) {
      this.nickname = payload.nickname || ''
      localStorage.setItem('cm_nickname', this.nickname)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      this.role = ''
      localStorage.removeItem('cm_token')
      localStorage.removeItem('cm_username')
      localStorage.removeItem('cm_nickname')
      localStorage.removeItem('cm_role')
    }
  }
})
