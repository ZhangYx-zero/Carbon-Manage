import request from '@/utils/request'

export const pageOrders = (params: any) => request.get('/orders/page', { params })

export const getOrder = (id: number) => request.get(`/orders/${id}`)

export const dispatchOrder = (id: number, data: any) => request.post(`/orders/${id}/dispatch`, data)

export const submitOrder = (id: number, data: any) => request.post(`/orders/${id}/submit`, data)

export const passOrder = (id: number, data: any) => request.post(`/orders/${id}/audit/pass`, data)

export const rejectOrder = (id: number, data: any) => request.post(`/orders/${id}/audit/reject`, data)
