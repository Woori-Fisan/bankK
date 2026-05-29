import React, { useState } from 'react';
import PinpadModal from '../components/pinpad/PinpadModal';

const PinpadTestPage: React.FC = () => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [enteredPin, setEnteredPin] = useState('');

    const handleComplete = (pin: string) => {
        setEnteredPin(pin);
        setIsModalOpen(false);
        alert(`입력된 핀번호: ${pin}`);
    };

    return (
        <div className="flex-1 p-8 flex flex-col items-center justify-center min-h-[600px]">
            <h1 className="text-3xl font-bold mb-8 text-slate-900">핀패드 컴포넌트 테스트</h1>
            
            <div className="bg-white p-10 rounded-2xl shadow-sm border border-slate-200 text-center max-w-md w-full">
                <p className="text-slate-600 mb-6 font-medium">
                    아래 버튼을 클릭하여 보안 핀패드 모달을 실행하세요.
                </p>
                
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="bg-emerald-700 hover:bg-emerald-800 text-white font-bold py-4 px-8 rounded-xl transition-all shadow-lg hover:shadow-emerald-900/20 active:scale-95"
                >
                    핀패드 열기
                </button>

                {enteredPin && (
                    <div className="mt-8 pt-8 border-t border-slate-100">
                        <p className="text-sm text-slate-400 mb-1">마지막 입력 결과</p>
                        <p className="text-xl font-mono font-bold text-emerald-700 tracking-widest">
                            {enteredPin.replace(/./g, '*')}
                        </p>
                    </div>
                )}
            </div>

            <PinpadModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onComplete={handleComplete}
                title="거래 비밀번호 확인"
            />
        </div>
    );
};

export default PinpadTestPage;
