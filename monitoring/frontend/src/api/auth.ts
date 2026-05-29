import type { LoginApiResponse, LoginRequest, RegisterRequest } from '../types/auth';
import type { ApiCommonResponse } from '../types/common';
import axios from 'axios';
import { axiosInstance, axiosTokenInstance } from './axiosInstance';

export const register = async (request: RegisterRequest): Promise<ApiCommonResponse<void>> => {
    const response = await axiosInstance.post<ApiCommonResponse<void>>('/user/register', request);
    return response.data;
};

export const login = async (request: LoginRequest): Promise<LoginApiResponse> => {
    const response = await axiosInstance.post<LoginApiResponse>('/user/login', request);
    return response.data;
};

export const logout = async (): Promise<ApiCommonResponse<void>> => {
    const response = await axiosTokenInstance.post<ApiCommonResponse<void>>('/user/logout');
    return response.data;
};

// 중복 리프레시 요청을 방지하기 위한 변수
let refreshPromise: Promise<string | null> | null = null;

export const refreshAccessToken = async (): Promise<string | null> => {
    // 이미 리프레시가 진행 중이라면 기존의 Promise를 반환하여 결과를 공유합니다.
    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise = (async () => {
        try {
            const response = await axios.post('http://localhost:8082/user/refresh', {}, {
                withCredentials: true
            });
            
            const responseData = response.data?.data || response.data;
            return responseData.accessToken || null;
        } catch (error) {
            console.error('Token Refresh Error:', error);
            return null;
        } finally {
            // 요청이 완료되면 변수를 초기화합니다.
            refreshPromise = null;
        }
    })();

    return refreshPromise;
};