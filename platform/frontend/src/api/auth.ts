import axios from 'axios';
import axiosInstance from './axiosInstance';

interface LoginResponse {
    success: boolean;
    message?: string;
    accessToken?: string;
    refreshToken?: string;
}

// 20자리의 고유 거래 번호 생성 (임시)
const generateTransactionId = (): string => {
    return Math.random().toString(36).substring(2, 22); // 20자리를 맞추기 위해 조정
};

export const login = async (
    employeeId: string,
    encryptedPassword: string,
    jwsSignature: string
): Promise<LoginResponse> => {
    try {
        const transactionId = generateTransactionId();
        const response = await axios.post('/api/v1/auth/login', {
            employeeId,
            password: encryptedPassword,
        }, {
            headers: {
                'Content-Type': 'application/json',
                'x-api-tran-id': transactionId,
                'x-jws-signature': jwsSignature,
            },
        });

        // 백엔드 ApiResponse 구조를 고려하여 data 추출 (response.data.data 안에 실제 DTO 존재)
        const responseData = response.data?.data || response.data;

        if (responseData && responseData.accessToken) {
            // Note: refreshToken is now expected to be handled via HttpOnly cookie
            return { success: true, message: '로그인 성공', ...responseData };
        } else {
            return { success: false, message: '로그인 실패: 응답에 액세스 토큰이 없습니다.' };
        }
    } catch (error: any) {
        console.error('Login API Error:', error);
        let errorMessage = '로그인 요청 중 오류가 발생했습니다.';
        if (axios.isAxiosError(error) && error.response) {
            if (error.response.status === 400 && error.response.data.code === 4000) {
                errorMessage = '필수 항목이 누락되었습니다.';
            } else if (error.response.status === 401) {
                errorMessage = '인증 실패: 직원 사번 또는 비밀번호가 올바르지 않습니다.';
            } else if (error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message;
            }
        }
        return { success: false, message: errorMessage };
    }
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
            const response = await axios.post('/api/v1/auth/refresh', {}, {
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

export const logoutApi = async (): Promise<boolean> => {
    try {
        await axiosInstance.post('/auth/logout');
        return true;
    } catch (error) {
        console.error('Logout API Error:', error);
        return false;
    }
};

// 기존 fetchBankPublicKey는 STACK_PLATFORM_FE.md에 따라 유지될 수 있으나,
// LOGIN.md는 플랫폼 로그인에 대한 명세이므로 이 Task에서는 직접적인 관련은 없습니다.
// 필요하다면 bankKeyStore 캐싱 로직은 별도 Task에서 다룰 수 있습니다.
export const fetchBankPublicKey = async (): Promise<string> => {
    await new Promise(resolve => setTimeout(resolve, 500));
    return 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...actual_bank_public_key_from_server...';
};
