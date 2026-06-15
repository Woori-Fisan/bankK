import React from 'react';
import { XCircle, AlertTriangle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTransferStore } from '../../../store/useTransferStore';
import { Button } from '../../common/Button';
import { formatAmount } from '../../../utils/formatter';

const TransferFailureView: React.FC = () => {
    const navigate = useNavigate();
    const { resultType, transactionId, amount, toName, reset } = useTransferStore();

    const isTimeout = resultType === 'TIMEOUT';

    const handleHome = () => {
        reset();
        navigate('/main');
    };

    return (
        <div className="w-full max-w-lg mx-auto flex flex-col items-center justify-center min-h-[60vh] space-y-8 animate-in fade-in duration-500">
            {/* 아이콘 */}
            <div className={`w-24 h-24 rounded-full flex items-center justify-center ${isTimeout ? 'bg-amber-50' : 'bg-red-50'}`}>
                {isTimeout
                    ? <AlertTriangle className="w-12 h-12 text-amber-500" />
                    : <XCircle className="w-12 h-12 text-red-500" />
                }
            </div>

            {/* 제목 및 설명 */}
            <div className="text-center space-y-3">
                <h2 className="text-2xl font-black text-slate-900">
                    {isTimeout ? '이체 처리가 지연되고 있습니다.' : '이체에 실패하였습니다.'}
                </h2>
                <p className="text-slate-500 font-medium leading-relaxed">
                    {isTimeout
                        ? '이체 결과를 확인하는 데 시간이 걸리고 있습니다.\n아래 거래 번호를 메모하여 고객센터에 문의해 주세요.'
                        : '출금 계좌로 환불이 완료되었습니다.'}
                </p>
            </div>

            {/* 이체 정보 카드 */}
            <div className="w-full bg-slate-50 rounded-3xl p-6 border border-slate-100 space-y-4">
                {isTimeout && (
                    <div className="flex justify-between items-center">
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">거래 번호</span>
                        <span className="text-sm font-black text-slate-900 font-mono">{transactionId}</span>
                    </div>
                )}
                <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">이체 금액</span>
                    <span className="text-lg font-black text-slate-900">₩ {formatAmount(amount)}</span>
                </div>
                <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">수취인</span>
                    <span className="text-base font-bold text-slate-700">{toName}</span>
                </div>
            </div>

            {isTimeout && (
                <p className="text-xs text-amber-600 font-medium text-center bg-amber-50 rounded-2xl px-5 py-3 border border-amber-100">
                    거래가 실제로 처리되었을 수 있습니다. 반드시 고객센터에 거래 번호를 알려 주세요.
                </p>
            )}

            <Button
                variant="primary"
                size="xl"
                fullWidth
                onClick={handleHome}
                className="rounded-2xl h-14 text-base font-bold shadow-lg shadow-slate-200"
            >
                메인 페이지로 돌아가기
            </Button>
        </div>
    );
};

export default TransferFailureView;
