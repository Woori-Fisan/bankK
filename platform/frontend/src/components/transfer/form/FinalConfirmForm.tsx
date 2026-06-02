import React, { useState } from 'react';
import { AlertCircle, User, Wallet } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import PageHeader from '../../common/PageHeader';
import PinpadModal from '../../pinpad/PinpadModal';
import { useTransferStore } from '../../../store/useTransferStore';
import { executeTransfer } from '../../../api/transfer';
import { formatAmount } from '../../../utils/formatter';

const FinalConfirmForm: React.FC = () => {
    const { 
        fromBank, fromBankName, fromAccountNumber, customerRrnPrefix,
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
        <div className="w-full max-w-4xl mx-auto space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-500 pb-10">
            <PageHeader 
                title="이체 정보 확인" 
                description="이체 실행 전 최종 정보를 확인해 주세요."
            />

            <Card padding="xl" className="border-slate-100 shadow-xl shadow-slate-200/50">
                <div className="space-y-10">
                    <div className="space-y-8 px-2">
                        {/* 2. 받는 분 정보 */}
                        <div className="space-y-3">
                            <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">받는 분</h3>
                            <div className="p-6 bg-emerald-50/50 rounded-3xl border border-emerald-100 flex items-center justify-between">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center text-emerald-600 shadow-sm border border-emerald-100">
                                        <User className="w-6 h-6" />
                                    </div>
                                    <div>
                                        <p className="text-lg font-black text-slate-900">{toName}</p>
                                        <p className="text-sm text-emerald-600 font-bold mt-0.5">{toBankName} {toAccountNumber}</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 3. 보내는 분 정보 (내 계좌 및 잔액) */}
                        <div className="space-y-3">
                            <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">보내는 분</h3>
                            <div className="p-6 bg-slate-50 rounded-3xl border border-slate-100">
                                <div className="flex items-center justify-between mb-4">
                                    <div>
                                        <p className="text-lg font-black text-slate-900">{fromBankName}</p>
                                        <p className="text-sm text-slate-500 font-mono mt-0.5">{fromAccountNumber}</p>
                                    </div>
                                    <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center text-slate-400 shadow-inner">
                                        <Wallet className="w-6 h-6" />
                                    </div>
                                </div>
                                <div className="pt-4 border-t border-slate-200/50 flex justify-between items-baseline">
                                    <span className="text-[10px] font-bold text-slate-400 uppercase">출금 가능 잔액</span>
                                    <span className="text-xl font-black text-emerald-600">원 {formatAmount(balance)}</span>
                                </div>
                            </div>
                        </div>

                        {/* 3. 최종 금액 요약 */}
                        <div className="space-y-3">
                            <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest text-center">최종 이체 금액</h3>
                            <div className="p-10 bg-white border-2 border-slate-100 rounded-[40px] text-center shadow-sm">
                                <div className="mb-6 flex items-center justify-center gap-4 text-sm font-bold">
                                    <div className="flex items-baseline gap-1 text-slate-400">
                                        <span>이체 신청 금액</span>
                                        <span>{formatAmount(amount)}원</span>
                                    </div>
                                    <div className="w-px h-3 bg-slate-200"></div>
                                    <div className="flex items-baseline gap-1 text-slate-400">
                                        <span>수수료</span>
                                        <span>0원 (면제)</span>
                                    </div>
                                </div>

                                <div className="flex items-baseline justify-center gap-2">
                                    <span className="text-xl font-bold text-slate-400">₩</span>
                                    <span className="text-7xl font-black text-slate-900 tracking-tighter">
                                        {formatAmount(amount)}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {error && (
                        <div className="px-6 py-4 bg-rose-50 border border-rose-100 rounded-2xl text-rose-600 text-sm font-bold flex items-center gap-2 mx-2">
                            <AlertCircle className="w-5 h-5" />
                            {error}
                        </div>
                    )}

                    {/* 4. 액션 버튼 */}
                    <footer className="mt-10 pt-8 border-t border-slate-50 grid grid-cols-1 md:grid-cols-4 gap-4 px-2">
                        <div className="md:col-span-1">
                            <Button
                                variant="secondary"
                                size="xl"
                                onClick={prevStep}
                                disabled={isLoading}
                                className="w-full rounded-2xl h-16 text-lg font-bold border-none shadow-sm hover:bg-slate-200"
                            >
                                정보 수정
                            </Button>
                        </div>
                        <div className="md:col-span-3">
                            <Button
                                variant="emerald"
                                size="xl"
                                onClick={() => setIsPinpadOpen(true)}
                                disabled={isLoading}
                                className="w-full rounded-2xl h-16 text-xl font-black shadow-xl shadow-emerald-900/20"
                            >
                                {isLoading ? '이체 진행 중...' : '이체 진행'}
                            </Button>
                        </div>
                    </footer>
                </div>
            </Card>

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
