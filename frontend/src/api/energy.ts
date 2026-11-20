import request from '@/utils/request'

export const pageEnergy = (params: any) => request.get('/energy/page', { params })

export const getEnergy = (id: number) => request.get(`/energy/${id}`)

export const collectEnergy = () => request.post('/energy/collect')
