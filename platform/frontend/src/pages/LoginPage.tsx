import React from 'react';
import LoginHeader from '../components/login/LoginHeader';
import LoginForm from '../components/login/LoginForm';
import SecurityStatus from '../components/login/SecurityStatus';

const LoginPage: React.FC = () => {
    return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-lg w-full max-w-sm p-8 flex flex-col items-center">
                <LoginHeader />
                <LoginForm />
                <SecurityStatus />
            </div>
        </div>
    );
};

export default LoginPage;
