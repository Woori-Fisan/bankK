import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: '/api/v1',
});

axiosInstance.interceptors.request.use((config) => {
  const token =
    sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const errorCode = error.response?.data?.error?.code;

    // 401, 403 에러 중 플랫폼 인증/권한 관련 에러(AUTH_*)인 경우에만 로그인으로 리다이렉트
    // errorCode가 없는 일반적인 401/403 에러도 세션 만료로 간주하여 포함
    const isAuthError = !errorCode || errorCode.startsWith('AUTH_');

    if ((status === 401 || status === 403) && isAuthError) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      sessionStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export default axiosInstance;
