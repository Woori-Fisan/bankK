import React, { useState } from 'react';
import { UserRound, Lock, BadgeInfo } from 'lucide-react';
import { Link } from 'react-router-dom';
import LoginInput from './LoginInput';

const SignupForm: React.FC = () => {
    const [name, setName] = useState('');
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        console.log('회원가입 버튼 누름', { loginId, password, name });
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
