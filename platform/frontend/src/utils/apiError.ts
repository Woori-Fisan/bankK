import { AxiosError } from 'axios';

/**
 * 서버 혹은 네트워크 에러 객체로부터 실제 메시지를 추출합니다.
 */
export const extractApiErrorMessage = (
  error: unknown, 
  defaultMessage: string = '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
): string => {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as AxiosError<any>;
    if (axiosError.response?.data?.error?.message) {
      return axiosError.response.data.error.message;
    }
    if (axiosError.response?.data?.message) {
      return axiosError.response.data.message;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return defaultMessage;
};
