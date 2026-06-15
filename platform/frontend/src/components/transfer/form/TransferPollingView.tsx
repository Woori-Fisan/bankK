import React, { useCallback } from 'react';
import { ArrowRightLeft } from 'lucide-react';
import { useTransferStore } from '../../../store/useTransferStore';
import { useTransferPolling } from '../../../hooks/useTransferPolling';
import { formatAmount } from '../../../utils/formatter';
import Card from '../../common/Card';

const TransferPollingView: React.FC = () => {
    const { transactionId, fromBank, amount, toName, toBankName, toAccountNumber } = useTransferStore();

    const onSuccess = useCallback(() => {
        const store = useTransferStore.getState();
        if (!store.transactionDate) {
            store.updateData({ transactionDate: new Date().toISOString() });
        }
        store.setStep(8);
    }, []);

    const onFailed = useCallback(() => {
        useTransferStore.getState().updateData({ resultType: 'FAILED' });
        useTransferStore.getState().setStep(9);
    }, []);

    const onTimeout = useCallback(() => {
        useTransferStore.getState().updateData({ resultType: 'TIMEOUT' });
        useTransferStore.getState().setStep(9);
    }, []);

    useTransferPolling({
        transactionId,
        withdrawalBankCode: fromBank,
        onSuccess,
        onFailed,
        onTimeout,
    });

    return (
        <div className="w-full max-w-lg mx-auto flex flex-col items-center justify-center min-h-[60vh] space-y-8 animate-in fade-in duration-500">
            {/* 스피너 */}
            <div className="relative">
                <div className="w-24 h-24 rounded-full border-4 border-emerald-100 border-t-emerald-500 animate-spin" />
                <div className="absolute inset-0 flex items-center justify-center">
                    <ArrowRightLeft className="w-8 h-8 text-emerald-500" />
                </div>
            </div>

            {/* 안내 문구 */}
            <div className="text-center space-y-2">
                <h2 className="text-2xl font-black text-slate-900">이체를 처리 중입니다</h2>
                <p className="text-slate-500 font-medium">잠시만 기다려 주세요.</p>
            </div>

            {/* 이체 요약 (안심 정보) */}
            <Card padding="lg" className="w-full border-slate-100 shadow-sm">
                <div className="space-y-4">
                    <div className="flex justify-between items-center">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">이체 금액</span>
                        <span className="text-2xl font-black text-slate-900">₩ {formatAmount(amount)}</span>
                    </div>
                    <div className="h-px bg-slate-100" />
                    <div className="flex justify-between items-center">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">수취인</span>
                        <span className="text-base font-black text-emerald-600">{toName}</span>
                    </div>
                    <div className="flex justify-between items-center">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">수취 계좌</span>
                        <div className="text-right">
                            <p className="text-sm font-bold text-slate-600">{toBankName}</p>
                            <p className="text-sm font-bold text-slate-900">{toAccountNumber}</p>
                        </div>
                    </div>
                </div>
            </Card>

            <p className="text-xs text-slate-400 text-center">
                이 화면을 벗어나지 말아 주세요. 이체 처리가 계속 진행됩니다.
            </p>
        </div>
    );
};

export default TransferPollingView;
