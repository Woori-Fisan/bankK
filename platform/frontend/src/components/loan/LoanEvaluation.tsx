import React, { useState, useEffect } from 'react';
import { Loader2, CheckCircle2, XCircle, Info, ChevronRight } from 'lucide-react';
import { LoanData, EvaluationResult } from '../../pages/LoanApplication';

interface LoanEvaluationProps {
    loanData: LoanData;
    onApproved: (result: EvaluationResult) => void;
    onRejected: (reason: string) => void;
}

const LoanEvaluation: React.FC<LoanEvaluationProps> = ({ loanData, onApproved, onRejected }) => {
    const [status, setStatus] = useState<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING');
    const [countdown, setCountdown] = useState(5);

    useEffect(() => {
        if (status !== 'PENDING') return;

        const timer = setInterval(() => {
            setCountdown(prev => {
                if (prev <= 1) return 5;
                return prev - 1;
            });
        }, 1000);

        // Simulate evaluation after 10 seconds
        const evaluationTimer = setTimeout(() => {
            const isSuccess = Math.random() > 0.2; // 80% success rate for mock
            if (isSuccess) {
                setStatus('APPROVED');
            } else {
                setStatus('REJECTED');
            }
        }, 10000);

        return () => {
            clearInterval(timer);
            clearTimeout(evaluationTimer);
        };
    }, [status]);

    const mockResult: EvaluationResult = {
        limit: 65000000,
        rate: 3.51,
        period: 24,
        bank: '우리은행',
        account: '100-293-884920',
        products: [
            { id: 1, name: '우리은행 직장인 신용대출 I', rate: 3.51, limit: 65000000, tags: ['최저금리', '1금융권'] },
            { id: 2, name: '우리은행 직장인 신용대출 II', rate: 3.83, limit: 50000000, tags: ['1금융권'] },
            { id: 3, name: '우리은행 공무원 교직원 신용대출', rate: 4.01, limit: 65000000, tags: ['1금융권'] },
        ]
    };

    if (status === 'PENDING') {
        return (
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl">
                <div className="relative mb-6">
                    <Loader2 className="w-16 h-16 text-emerald-500 animate-spin" />
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="w-8 h-8 bg-emerald-50 rounded-full"></div>
                    </div>
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">심사 중입니다</h2>
                <p className="text-sm text-gray-500 text-center max-w-xs mb-6">
                    잠시만 기다려 주세요. 심사 결과를 자동으로 조회합니다.<br/>
                    <span className="text-[11px] opacity-70">Application ID: L-20231024-8839</span>
                </p>
                <div className="flex gap-1.5 mb-6">
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse"></div>
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse delay-75"></div>
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse delay-150"></div>
                </div>
                <p className="text-[11px] text-gray-400 font-medium">
                    {countdown}초마다 자동 갱신 중...
                </p>
            </div>
        );
    }

    if (status === 'REJECTED') {
        return (
            <div className="flex flex-col items-center justify-center py-20 bg-white border border-gray-200 rounded-3xl">
                <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-6">
                    <XCircle className="w-8 h-8 text-red-500" />
                </div>
                <h2 className="text-xl font-bold text-gray-900 mb-2">대출 심사가 거절되었습니다</h2>
                <p className="text-sm text-gray-500 mb-8 tracking-tight">Application ID: L-20231024-8839</p>
                
                <div className="w-full max-w-sm bg-red-50 border border-red-100 rounded-2xl p-6 text-left">
                    <h3 className="text-xs font-bold text-red-600 mb-3 uppercase tracking-wider">거절 사유</h3>
                    <p className="text-sm text-red-800 font-medium leading-relaxed">
                        신용점수 미달 (조회 점수: 580점 / 기준: 600점 이상)
                    </p>
                </div>
                
                <button 
                    onClick={() => onRejected('신용점수 미달')}
                    className="mt-10 px-8 py-3 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors"
                >
                    목록으로 돌아가기
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="bg-white border border-gray-200 rounded-2xl p-6">
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h2 className="text-xl font-bold text-gray-900">대출 심사 결과</h2>
                        <p className="text-[11px] text-gray-500 mt-1 uppercase tracking-tight">Application ID: L-20231024-8839</p>
                    </div>
                    <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-600 rounded-full border border-emerald-100">
                        <CheckCircle2 className="w-4 h-4" />
                        <span className="text-xs font-bold">승인</span>
                    </div>
                </div>

                <div className="bg-gray-50 rounded-2xl p-8 text-center mb-6 border border-gray-100">
                    <p className="text-xs text-gray-500 mb-2">최종 승인 한도</p>
                    <p className="text-4xl font-bold text-gray-900">₩ 65,000,000</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">적용 금리</p>
                        <p className="text-lg font-bold text-gray-900">4.25%</p>
                        <p className="text-[10px] text-gray-500">고정 / 연</p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">대출 기간</p>
                        <p className="text-lg font-bold text-gray-900">36 <span className="text-sm font-normal text-gray-500">개월</span></p>
                    </div>
                    <div className="bg-gray-50 border border-gray-100 rounded-xl p-4">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">입금 계좌</p>
                        <p className="text-sm font-bold text-gray-900">{mockResult.bank}</p>
                        <p className="text-[10px] text-gray-500">{mockResult.account}</p>
                    </div>
                </div>

                <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-5">
                    <div className="flex items-center gap-2 text-emerald-700 font-bold text-xs mb-4">
                        <Info className="w-4 h-4" />
                        다음 단계 안내
                    </div>
                    <div className="space-y-4">
                        {[
                            { step: 1, title: '추천 상품 선택', desc: '심사 결과 기준 추천 상품 목록에서 상품을 선택합니다.' },
                            { step: 2, title: '계약 서류 확인 및 동의', desc: '선택한 상품의 약관 및 계약 서류를 확인하고 동의합니다.' },
                            { step: 3, title: '대출 실행', desc: '동의 완료 후 대출금이 지정 계좌로 입금됩니다.' },
                        ].map((item) => (
                            <div key={item.step} className="flex gap-3">
                                <div className="w-5 h-5 bg-emerald-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center shrink-0">
                                    {item.step}
                                </div>
                                <div>
                                    <p className="text-xs font-bold text-gray-900 mb-0.5">{item.title}</p>
                                    <p className="text-[10px] text-gray-500">{item.desc}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="flex justify-end pt-6 border-t border-gray-200">
                <button 
                    onClick={() => onApproved(mockResult)}
                    className="flex items-center gap-2 px-6 py-3 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200"
                >
                    상품 선택하기
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};

export default LoanEvaluation;
