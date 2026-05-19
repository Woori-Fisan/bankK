import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserRound, Lock } from 'lucide-react';
import LoginInput from './LoginInput';
import { useAuthCrypto } from '../../hooks/useAuthCrypto';
import { login } from '../../api/auth';
import { fetchPlatformPublicKey } from '../../utils/authCrypto';

const LoginForm: React.FC = () => {
    const navigate = useNavigate();

    const [employeeId, setEmployeeId] = useState('');
    const [password, setPassword] = useState('');
    const [loginMessage, setLoginMessage] = useState<string | null>(null);
    const [loginSuccess, setLoginSuccess] = useState<boolean | null>(null);
    const [platformPublicKey, setPlatformPublicKey] = useState<string | null>(null);
    const { encryptAndSign, isLoading: isAuthProcessing, error: authError } = useAuthCrypto();

    useEffect(() => {
        const getPublicKey = async () => {
            try {
                const key = await fetchPlatformPublicKey();
                setPlatformPublicKey(key);
            } catch (err: any) {
                console.error('Failed to fetch platform public key:', err);
                setLoginMessage('플랫폼 공개키를 가져오는데 실패했습니다.');
                setLoginSuccess(false);
            }
        };
        getPublicKey();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoginMessage(null);
        setLoginSuccess(null);

        if (!platformPublicKey) {
            setLoginMessage('플랫폼 공개키를 불러오는 중입니다. 잠시 후 다시 시도해주세요.');
            setLoginSuccess(false);
            return;
        }
        if (!employeeId || !password) {
            setLoginMessage('아이디와 비밀번호를 모두 입력해주세요.');
            setLoginSuccess(false);
            return;
        }

        const loginPayload = { employeeId, password }; // JWS 서명을 위한 원본 payload

        const authResult = await encryptAndSign(password, loginPayload, platformPublicKey);

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
            setLoginMessage(`인증 처리 실패: ${authError}`);
            setLoginSuccess(false);
        }
    };

    const isSubmitting = isAuthProcessing;

    return (
        <form className="w-full space-y-6" onSubmit={handleSubmit}>
            <LoginInput
                label="아이디"
                icon={UserRound}
                type="text"
                placeholder="아이디를 입력하세요"
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
                className="w-full bg-emerald-800 hover:bg-emerald-900 text-white font-bold py-3 px-4 rounded-lg transition-colors mt-2 shadow-sm"
                disabled={isSubmitting || !platformPublicKey}
            >
                {isSubmitting ? '로그인 중...' : '로그인'}
            </button>
            {loginMessage && (
                <p className={`text-center mt-2 ${loginSuccess ? 'text-green-600' : 'text-red-500'}`}>
                    {loginMessage}
                </p>
            )}
            {authError && !loginMessage && (
                 <p className="text-red-500 text-center mt-2">인증 오류: {authError}</p>
            )}
        </form>
    );
};

export default LoginForm;