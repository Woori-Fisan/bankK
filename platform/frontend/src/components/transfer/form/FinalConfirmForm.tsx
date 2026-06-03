import React, { useState } from 'react';
import { AlertCircle, AlertTriangle } from 'lucide-react';
import { Button } from '../../common/Button';
import PinpadModal from '../../pinpad/PinpadModal';
import { useTransferStore } from '../../../store/useTransferStore';
import { executeTransfer } from '../../../api/transfer';
import { formatAmount } from '../../../utils/formatter';

const FinalConfirmForm: React.FC = () => {
    const { 
        fromName, fromBank, fromBankName, fromAccountNumber, customerRrnPrefix,
        toBank, toBankName, toAccountNumber, toName, amount, balance,
        prevStep, nextStep, updateData 
    } = useTransferStore();
    const [isPinpadOpen, setIsPinpadOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handlePinComplete = async (pin: string) => {
        setIsPinpadOpen(false);
        setIsLoading(true);
        setError(null);

        try {
            const response = await executeTransfer({
                withdrawalBankCode: fromBank,
                withdrawalAccountNo: fromAccountNumber,
                withdrawalPassword: pin,
                customerRrnPrefix: customerRrnPrefix,
                depositBankCode: toBank,
                depositAccountNo: toAccountNumber,
                amount: amount,
                encryptedKey: 'TEMP_ENCRYPTED_KEY',
                jwsSignature: 'TEMP_JWS_SIGNATURE'
            });

            if (response.success) {
                updateData({
                    transactionId: response.data.transactionId,
                    transactionDate: response.data.transactionDate,
                    balanceAfter: response.data.balanceAfter
                });
                nextStep();
            } else {
                setError(response.error?.message || '이체 실행에 실패했습니다.');
            }
        } catch (err: any) {
            setError('서버 통신 중 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="w-full">
            {/* 헤더 */}
            <div className="bg-emerald-600 px-10 py-8 flex items-center gap-6">
                <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-md">
                    <AlertTriangle className="w-8 h-8 text-white" />
                </div>
                <div>
                    <h3 className="text-2xl font-black text-white">최종 이체 확인</h3>
                    <p className="text-emerald-100 mt-1 font-medium">
                        입력하신 이체 정보가 정확한지 확인해 주세요.
                    </p>
                </div>
            </div>

            <div className="p-10 space-y-8">
                <div className="bg-slate-50 rounded-3xl p-8 space-y-5 border border-slate-100">
                    {/* 받는 분 */}
                    <div className="flex justify-between items-start">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mt-1">받는 분</span>
                        <div className='flex gap-15'>
                            <span className="text-lg font-black text-emerald-600">{toName}</span>
                            <div className="text-right">
                                <p className="text-sm font-bold text-emerald-600">{toBankName}</p>
                                <p className="text-lg font-black text-slate-900">{toAccountNumber}</p>
                            </div>
                        </div>
                    </div>
                    
                    <div className="h-px bg-slate-200/50" />

                    {/* 보내는 분 */}
                    <div className="flex justify-between items-start">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mt-1">보내는 분</span>
                        <div className='flex gap-15'>
                            <span className="text-lg font-black text-emerald-600">{fromName}</span>
                            <div className="text-right">
                                <p className="text-sm font-bold text-emerald-600">{fromBankName}</p>
                                <p className="text-lg font-black text-slate-900">{fromAccountNumber}</p>
                            </div>
                        </div>
                    </div>

                    <div className="h-px bg-slate-200/50" />

                    {/* 이체 금액 */}
                    <div className="flex justify-between items-center pt-2">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">이체 금액</span>
                        <div className="text-right">
                            <p className="text-3xl font-black text-slate-900">₩ {formatAmount(amount)}</p>
                            <p className="text-xs text-slate-400 font-bold mt-1">수수료 면제 (0원)</p>
                        </div>
                    </div>

                    <div className="h-px bg-slate-200/30 mx-2" />

                    {/* 이체 후 예상 잔액 */}
                    <div className="flex justify-between items-center pt-2">
                        <span className="text-xs font-black text-slate-500 uppercase tracking-wider">이체 후 예상 잔액</span>
                        <div className="text-right">
                            <p className="text-2xl font-black text-emerald-600">
                                ₩ {formatAmount(Number(balance) - amount)}
                            </p>
                        </div>
                    </div>
                </div>

                {error && (
                    <div className="px-6 py-4 bg-rose-50 border border-rose-100 rounded-2xl text-rose-600 text-sm font-bold flex items-center gap-2">
                        <AlertCircle className="w-5 h-5 shrink-0" />
                        {error}
                    </div>
                )}
            </div>

            {/* 버튼 */}
            <div className="px-10 pb-10 grid grid-cols-2 gap-4">
                <Button
                    onClick={prevStep}
                    disabled={isLoading}
                    variant="secondary"
                    size="xl"
                    className="h-16 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200"
                >
                    수정하기
                </Button>
                <Button
                    onClick={() => setIsPinpadOpen(true)}
                    disabled={isLoading}
                    variant="primary"
                    size="xl"
                    className="h-16 rounded-2xl bg-slate-900 text-white hover:bg-slate-800 shadow-xl shadow-slate-900/20"
                >
                    {isLoading ? '처리 중...' : '확인 완료'}
                </Button>
            </div>

            <PinpadModal 
                isOpen={isPinpadOpen} 
                onClose={() => setIsPinpadOpen(false)} 
                onComplete={handlePinComplete}
                title="출금 비밀번호 입력"
            />
        </div>
    );
};

export default FinalConfirmForm;
