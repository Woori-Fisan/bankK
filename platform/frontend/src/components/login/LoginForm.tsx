import React from 'react';
import { UserRound, Lock } from 'lucide-react';
import LoginInput from './LoginInput';

const LoginForm: React.FC = () => {
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
    };

    return (
        <form className="w-full space-y-6" onSubmit={handleSubmit}>
            <LoginInput
                label="직원 사번 (ID)"
                icon={UserRound}
                type="text"
                placeholder="사번을 입력하세요"
            />
            <LoginInput
                label="비밀번호"
                icon={Lock}
                type="password"
                placeholder="비밀번호를 입력하세요"
            />
            <button
                type="submit"
                className="w-full bg-emerald-800 hover:bg-emerald-900 text-white font-bold py-3 px-4 rounded-lg transition-colors mt-2 shadow-sm"
            >
                로그인
            </button>
        </form>
    );
};

export default LoginForm;
