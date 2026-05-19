import React from 'react';
import WithdrawForm from '../components/withdraw/WithdrawForm';

const WithdrawPage: React.FC = () => {
    return (
        <div className="p-8 max-w-7xl mx-auto min-h-full flex flex-col bg-gray-50">
            <header className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900">출금 정보 입력</h1>
            </header>

            <main className="flex-1 flex justify-center items-start pt-4">
                <WithdrawForm />
            </main>
        </div>
    );
};

export default WithdrawPage;
