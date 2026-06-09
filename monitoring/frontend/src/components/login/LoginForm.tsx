import React, { useState } from 'react';
import { UserRound, Lock } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import LoginInput from './LoginInput';
import { login } from '../../api/auth';
import { decodeJwt } from '../../utils/jwt';
import { useAuthStore } from '../../store/useAuthStore';

const LoginForm: React.FC = () => {
    const navigate = useNavigate();
    const { setUserId, setAccessToken, setLoginTime, setTokenExpiry, clearAuth } = useAuthStore();

    const [employeeId, setEmployeeId] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        clearAuth();

        const trimmedId = employeeId.trim();
        if (trimmedId.length < 4 || trimmedId.length > 20) {
            setError('ID는 4~20자로 입력해주세요.');
            return;
        }
        if (password.trim().length < 8) {
            setError('비밀번호는 8자 이상 입력해주세요.');
            return;
        }

        setIsLoading(true);
        try {
            const response = await login({ loginId: employeeId, password });
            if (response.success) {
                setUserId(employeeId);
                localStorage.setItem('userId', employeeId);

                const now = new Date();
                const formattedTime =
                    now.getFullYear() + '.' +
                    String(now.getMonth() + 1).padStart(2, '0') + '.' +
                    String(now.getDate()).padStart(2, '0') + ' ' +
                    String(now.getHours()).padStart(2, '0') + ':' +
                    String(now.getMinutes()).padStart(2, '0') + ':' +
                    String(now.getSeconds()).padStart(2, '0');
                setLoginTime(formattedTime);
                localStorage.setItem('loginTime', formattedTime);

                const accessToken = response.data?.accessToken;
                const refreshTokenExpiresIn = response.data?.refreshTokenExpiresIn;
                if (accessToken) {
                    setAccessToken(accessToken);
                    if (refreshTokenExpiresIn) {
                        setTokenExpiry(Date.now() + refreshTokenExpiresIn * 1000);
                    } else {
                        const decoded = decodeJwt(accessToken);
                        if (decoded?.exp) {
                            setTokenExpiry(decoded.exp * 1000);
                        }
                    }
                }

                navigate('/dashboard');
            } else {
                setError(response.error?.message ?? '로그인에 실패했습니다.');
            }
        } catch {
            setError('서버 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <form className="w-full space-y-6" onSubmit={handleSubmit}>
            <LoginInput
                label="ID"
                icon={UserRound}
                type="text"
                placeholder="ID를 입력하세요"
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
            />
            <LoginInput
                label="비밀번호"
                icon={Lock}
                type="password"
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
            {error && (
                <p className="text-sm text-red-500 text-center">{error}</p>
            )}
            <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold py-4 px-4 rounded-xl transition-all mt-4 shadow-md hover:shadow-lg transform hover:-translate-y-0.5 active:scale-[0.98] disabled:opacity-50"
            >
                {isLoading ? '접속 중...' : '시스템 접속'}
            </button>
            <p className="text-center text-sm text-slate-500">
                계정이 없으신가요?{' '}
                <Link to="/signup" className="font-bold text-emerald-700 hover:text-emerald-800">
                    회원가입
                </Link>
            </p>
        </form>
    );
};

export default LoginForm;
