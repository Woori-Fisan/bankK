import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserRound, Lock } from 'lucide-react';
import LoginInput from './LoginInput';
import { useAuthCrypto } from '../../hooks/useAuthCrypto';
import { login } from '../../api/auth';

const LoginForm: React.FC = () => {
    const navigate = useNavigate();

    const [employeeId, setEmployeeId] = useState('');
    const [password, setPassword] = useState('');
    const [loginMessage, setLoginMessage] = useState<string | null>(null);
    const [loginSuccess, setLoginSuccess] = useState<boolean | null>(null);
    
    // 새로 수정한 useAuthCrypto 훅 사용
    const { encryptAndSign, isLoading: isAuthProcessing, error: authError } = useAuthCrypto();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoginMessage(null);
        setLoginSuccess(null);

        if (!employeeId || !password) {
            setLoginMessage('아이디와 비밀번호를 모두 입력해주세요.');
            setLoginSuccess(false);
            return;
        }

        // 1. 비밀번호 JWE 암호화 및 서명될 페이로드 JWS 서명 수행
        // 내부에서 평문 비밀번호를 암호화한 뒤 그 암호문을 서명 페이로드에 포함시킵니다.
        const authResult = await encryptAndSign(password, employeeId);

        // 2. 암호화 및 서명이 성공했을 경우에만 서버로 전송
        if (authResult && authResult.encryptedPassword && authResult.jwsSignature) {
            const response = await login(
                employeeId,
                authResult.encryptedPassword,
                authResult.jwsSignature
            );

            if (response.success) {
                setLoginMessage(response.message || '로그인 성공!');
                setLoginSuccess(true);
                navigate('/main');
            } else {
                setLoginMessage(response.message || '로그인 실패. 다시 시도해주세요.');
                setLoginSuccess(false);
            }
        } else {
            console.error('Authentication processing failed:', authError);
            setLoginMessage(`인증 처리 실패: 단말기 암호화 키를 등록해주세요.`);
            setLoginSuccess(false);
        }
    };

    const isSubmitting = isAuthProcessing;

    return (
        <form className="w-full space-y-6" onSubmit={handleSubmit}>
            <LoginInput
                label="사번"
                icon={UserRound}
                type="text"
                placeholder="사원번호를 입력하세요"
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
                disabled={isSubmitting}
            />
            <LoginInput
                label="비밀번호"
                icon={Lock}
                type="password"
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isSubmitting}
            />
            <button
                type="submit"
                className="w-full bg-emerald-800 hover:bg-emerald-900 text-white font-bold py-3 px-4 rounded-lg transition-colors mt-2 shadow-sm disabled:opacity-50"
                disabled={isSubmitting}
            >
                {isSubmitting ? '보안 인증 처리 중...' : '로그인'}
            </button>
            {loginMessage && (
                <p className={`text-center mt-2 text-sm font-semibold ${loginSuccess ? 'text-green-600' : 'text-red-500'}`}>
                    {loginMessage}
                </p>
            )}
            {authError && !loginMessage && (
                 <p className="text-red-500 text-center mt-2 text-sm font-semibold">인증 오류: {authError}</p>
            )}
        </form>
    );
};

export default LoginForm;