import request from '@/utils/request'

export const pageDiagnosis = (params: any) => request.get('/diagnosis/page', { params })

export const getDiagnosis = (id: number) => request.get(`/diagnosis/${id}`)

export const retryDiagnosis = (id: number) => request.post(`/diagnosis/${id}/retry`)

export const triggerDiagnosis = (energyDataId: number) => request.post(`/diagnosis/trigger/${energyDataId}`)
