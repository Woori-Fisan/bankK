import React, { useState } from 'react';
import { UserRound, Lock, BadgeInfo } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import LoginInput from './LoginInput';
import { register } from '../../api/auth';

const SignupForm: React.FC = () => {
    const [name, setName] = useState('');
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        if (name.trim().length < 1) {
            setError('이름을 입력해주세요.');
            return;
        }
        const trimmedId = loginId.trim();
        if (trimmedId.length < 4 || trimmedId.length > 20) {
            setError('ID는 4~20자로 입력해주세요.');
            return;
        }
        if (password.trim().length < 8) {
            setError('비밀번호는 8자 이상 입력해주세요.');
            return;
        }

        try {
            await register({ name, loginId, password });
            navigate('/login');
        } catch {
            setError('회원가입에 실패했습니다. 다시 시도해주세요.');
        }
    };

    return (
        <form className="w-full space-y-6" onSubmit={handleSubmit}>
            <LoginInput
                label="이름"
                icon={BadgeInfo}
                type="text"
                placeholder="이름을 입력하세요"
                value={name}
                onChange={(e) => setName(e.target.value)}
            />
            <LoginInput
                label="ID"
                icon={UserRound}
                type="text"
                placeholder="ID를 입력하세요"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
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
                <p className="text-red-500 text-sm text-center -mt-2">{error}</p>
            )}
            <button
                type="submit"
                className="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold py-4 px-4 rounded-xl transition-all mt-4 shadow-md hover:shadow-lg transform hover:-translate-y-0.5 active:scale-[0.98] disabled:opacity-50"
            >
                회원가입
            </button>
            <p className="text-center text-sm text-slate-500">
                이미 계정이 있으신가요?{' '}
                <Link to="/login" className="font-bold text-emerald-700 hover:text-emerald-800">
                    로그인
                </Link>
            </p>
        </form>
    );
};

export default SignupForm;
