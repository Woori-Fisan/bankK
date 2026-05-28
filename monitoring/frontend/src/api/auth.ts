import type { RegisterRequest } from '../types/auth';
import type { ApiCommonResponse } from '../types/common';
import axiosInstance from './axiosInstance';



export const register = async (request: RegisterRequest): Promise<ApiCommonResponse<void>> => {
    const response = await axiosInstance.post<ApiCommonResponse<void>>('/user/register', request);
    return response.data;
};