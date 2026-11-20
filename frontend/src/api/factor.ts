import request from '@/utils/request'

export const listFactors = () => request.get('/factors')

export const listEnabledFactors = () => request.get('/factors/list')

export const createFactor = (data: any) => request.post('/factors', data)

export const updateFactor = (id: number, data: any) => request.put(`/factors/${id}`, data)

export const deleteFactor = (id: number) => request.delete(`/factors/${id}`)
