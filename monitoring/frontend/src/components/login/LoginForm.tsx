import React, { useState } from 'react';
import { UserRound, Lock } from 'lucide-react';
import LoginInput from './LoginInput';


const LoginForm: React.FC = () => {

    const [employeeId, setEmployeeId] = useState('');
    const [password, setPassword] = useState('');
    

    const handleSubmit = async () => {
        console.log('로그인 버튼 누름');
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
            <button
                type="submit"
                className="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold py-4 px-4 rounded-xl transition-all mt-4 shadow-md hover:shadow-lg transform hover:-translate-y-0.5 active:scale-[0.98] disabled:opacity-50"
            >
                시스템 접속
            </button>
        </form>
    );
};

export default LoginForm;