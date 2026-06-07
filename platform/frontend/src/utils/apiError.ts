import axios from 'axios';

/**
 * 서버 혹은 네트워크 에러 객체로부터 실제 메시지를 추출합니다.
 */
export const extractApiErrorMessage = (
  error: unknown, 
  defaultMessage: string = '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
): string => {
  if (axios.isAxiosError(error)) {
    if (error.response?.data?.error?.message) {
      return error.response.data.error.message;
    }
    if (error.response?.data?.message) {
      return error.response.data.message;
    }
  }
  return defaultMessage;
};
