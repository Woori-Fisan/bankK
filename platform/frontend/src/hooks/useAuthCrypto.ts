import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { encryptPassword, createJwsSignature } from '../utils/authCrypto';
import { logoutApi } from '../api/auth';

interface AuthCryptoResult {
    encryptAndSign: (password: string, employeeId: string) => Promise<{ encryptedPassword: string; jwsSignature: string } | null>;
    performLogout: () => Promise<void>;
    isLoading: boolean;
    error: string | null;
}

export const useAuthCrypto = (): AuthCryptoResult => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const encryptAndSign = useCallback(async (password: string, employeeId: string) => {
        setIsLoading(true);
        setError(null);
        try {
            // 1. 비밀번호 JWE 암호화 (내부에서 자동으로 서버 공개키 조회)
            const encryptedPassword = await encryptPassword(password);
            if (!encryptedPassword) {
                throw new Error("비밀번호 암호화에 실패했습니다.");
            }

            // 2. 전체 페이로드 구성 (중요: 평문이 아닌 암호화된 비밀번호 사용)
            const payload = { 
                employeeId, 
                password: encryptedPassword,
                timestamp: Date.now() 
            };

            // 3. 전체 페이로드 JWS 서명 (내부에서 자동으로 IndexedDB 개인키 사용)
            const jwsSignature = await createJwsSignature(payload);
            if (!jwsSignature) {
                throw new Error("단말기 서명 생성에 실패했습니다. 단말기 키가 등록되어 있는지 확인해주세요.");
            }

            return { encryptedPassword, jwsSignature };
        } catch (e: any) {
            setError(e.message || "암호화 및 서명 처리 중 오류가 발생했습니다.");
            return null;
        } finally {
            setIsLoading(false);
        }
    }, []);

    const performLogout = useCallback(async () => {
        setIsLoading(true);
        try {
            // 1. 서버 로그아웃 API 호출
            await logoutApi();
        } finally {
            // 2. 로컬 토큰 데이터 삭제 (서버 통신 실패 여부와 무관하게 로컬은 삭제)
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            sessionStorage.removeItem('accessToken');
            
            setIsLoading(false);
            
            // 3. 로그인 페이지로 리다이렉트
            navigate('/login', { replace: true });
        }
    }, [navigate]);

    return { encryptAndSign, performLogout, isLoading, error };
};
