import type { LoginApiResponse, LoginRequest, RegisterRequest } from '../types/auth';
import type { ApiCommonResponse } from '../types/common';
import axiosInstance from './axiosInstance';

export const register = async (request: RegisterRequest): Promise<ApiCommonResponse<void>> => {
    const response = await axiosInstance.post<ApiCommonResponse<void>>('/user/register', request);
    return response.data;
};

export const login = async (request: LoginRequest): Promise<LoginApiResponse> => {
    const response = await axiosInstance.post<LoginApiResponse>('/user/login', request);
    return response.data;
};