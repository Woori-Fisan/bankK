import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';
import { refreshAccessToken } from './auth';

const axiosInstance = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
});

axiosInstance.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    
    // 401 에러이고 재시도하지 않은 요청인 경우 토큰 갱신 시도
    if (status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      const newAccessToken = await refreshAccessToken();
      
      if (newAccessToken) {
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return axiosInstance(originalRequest);
      }
      
      // 갱신 실패 시 로그아웃 처리
      useAuthStore.getState().clearAuth();
      window.location.href = '/login';
    }

    return Promise.reject(error);
  },
);

export default axiosInstance;
