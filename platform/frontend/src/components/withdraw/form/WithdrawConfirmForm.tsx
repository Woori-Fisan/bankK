import React from 'react';
import WithdrawConfirmSection from '../sections/WithdrawConfirmSection';
import type { WithdrawData } from '../../../types/withdraw';
import Card from '../../common/Card';
import { Button } from '../../common/Button';

export interface WithdrawConfirmFormProps {
    data: WithdrawData;
    onConfirm: () => void;
    onBack: () => void;
}

const WithdrawConfirmForm: React.FC<WithdrawConfirmFormProps> = ({
    data,
    onConfirm,
    onBack,
}) => {
    return (
        <div className="w-full max-w-7xl mx-auto space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-500">
            <Card padding="lg" className="border-slate-100 shadow-xl shadow-slate-200/50">
                <WithdrawConfirmSection 
                    sourceAccount={data.sourceAccount}
                    // birthDate={data.birthDate}
                    amount={data.amount}
                    fee={data.fee}
                />

                <footer className="mt-10 pt-8 border-t border-slate-50 grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="md:col-span-1">
                        <Button
                            variant="secondary"
                            size="xl"
                            onClick={onBack}
                            className="w-full rounded-2xl h-16 text-lg font-bold border-none shadow-sm hover:bg-slate-200"
                        >
                            정보 수정
                        </Button>
                    </div>
                    <div className="md:col-span-3">
                        <Button
                            variant="emerald"
                            size="xl"
                            onClick={onConfirm}
                            className="w-full rounded-2xl h-16 text-xl font-black shadow-xl shadow-emerald-900/20"
                        >
                            출금 진행
                        </Button>
                    </div>
                </footer>
            </Card>

            <div className="flex justify-center">
                <p className="text-slate-400 text-xs font-medium flex items-center gap-2">
                    <div className="w-1 h-1 rounded-full bg-slate-300" />
                    모든 거래 내역은 금융 보안 표준에 따라 암호화되어 처리됩니다.
                </p>
            </div>
        </div>
    );
};

export default WithdrawConfirmForm;
