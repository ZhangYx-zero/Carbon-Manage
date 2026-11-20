import axios from 'axios'
import { ElMessage } from 'element-plus'

const request: any = axios.create({
  baseURL: '/api',
  timeout: 120000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('cm_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('cm_token')
      localStorage.removeItem('cm_username')
      localStorage.removeItem('cm_nickname')
      localStorage.removeItem('cm_role')
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络异常，请检查后端服务')
    return Promise.reject(error)
  }
)

export default request
