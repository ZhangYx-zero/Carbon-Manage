import request from '@/utils/request'

export const listEnergyTypes = () => request.get('/energy-types')
export const listEnabledEnergyTypes = () => request.get('/energy-types/list')
export const createEnergyType = (data: any) => request.post('/energy-types', data)
export const updateEnergyType = (id: number, data: any) => request.put(`/energy-types/${id}`, data)
export const deleteEnergyType = (id: number) => request.delete(`/energy-types/${id}`)
