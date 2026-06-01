import React from 'react';
import LoginHeader from '../components/login/LoginHeader';
import SignupForm from '../components/login/SignupForm';

const SignupPage: React.FC = () => {
    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl shadow-xl w-full max-w-md p-10 flex flex-col items-center">
                <LoginHeader />
                <SignupForm />
            </div>
        </div>
    );
};

export default SignupPage;
