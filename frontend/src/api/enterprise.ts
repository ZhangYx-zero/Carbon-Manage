import request from '@/utils/request'

export const listEnterprises = (keyword?: string) =>
  request.get('/enterprises', { params: { keyword } })

export const listEnabledEnterprises = () => request.get('/enterprises/list')

export const createEnterprise = (data: any) => request.post('/enterprises', data)

export const updateEnterprise = (id: number, data: any) => request.put(`/enterprises/${id}`, data)

export const deleteEnterprise = (id: number) => request.delete(`/enterprises/${id}`)
