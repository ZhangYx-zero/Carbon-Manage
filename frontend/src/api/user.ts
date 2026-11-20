import request from '@/utils/request'

export const listOperators = () => request.get('/users/operators')

export const listUsers = () => request.get('/users')

export const createUser = (data: any) => request.post('/users', data)

export const updateProfile = (data: any) => request.put('/users/profile', data)

export const changePassword = (data: any) => request.put('/users/password', data)
