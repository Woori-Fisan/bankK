import React from 'react';
import { ArrowRight, User, Info, CheckCircle2 } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import PageHeader from '../../common/PageHeader';
import { useTransferStore } from '../../../store/useTransferStore';

const RecipientConfirmForm: React.FC = () => {
    const { toName, toBankName, toBankAccountNo, prevStep, nextStep } = useTransferStore();

    return (
        <div className="w-full">
            <PageHeader 
                title="수취인 정보 확인" 
                description="입력하신 계좌의 수취인 정보를 확인해 주세요."
            />

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* 1. 정보 확인 영역 (2컬럼) */}
                <div className="lg:col-span-2 space-y-8">
                    <Card padding="lg" className="border-slate-100 shadow-sm">
                        <div className="space-y-10">
                            {/* 안내 문구 */}
                            <div className="px-6 py-4 bg-emerald-50/50 rounded-2xl border border-emerald-100 flex items-center gap-3">
                                <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                                <p className="text-xs text-emerald-700 font-bold">수취인 정보가 맞는지 다시 한번 확인해 주세요.</p>
                            </div>

                            {/* 수취인 정보 박스 */}
                            <div className="space-y-4">
                                <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest">수취인(받는 분) 정보</h3>
                                <div className="p-8 bg-slate-50 rounded-3xl border border-slate-100">
                                    <p className="text-xl font-bold text-emerald-600">{toName}</p>
                                    <p className="text-lg font-bold text-slate-600 mt-2">{toBankName}</p>
                                    <p className="text-2xl font-black text-slate-900 tracking-tight mt-1 font-mono">{toBankAccountNo}</p>
                                </div>
                            </div>
                        </div>
                    </Card>

                    <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                        <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                        <p className="text-xs text-slate-500 leading-relaxed font-medium">
                            수취인 정보가 다를 경우 이체가 반환될 수 있습니다. 성명과 계좌번호를 대조하여 확인해 주세요.
                        </p>
                    </div>
                </div>

                {/* 2. 실행 영역 (1컬럼) */}
                <div className="lg:col-span-1 space-y-6">
                    <Card padding="lg" className="bg-emerald-900 text-white border-none shadow-xl shadow-emerald-900/10 flex flex-col justify-between min-h-[200px]">
                        <div>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-black text-emerald-300 uppercase tracking-widest">상태 확인</span>
                                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                            </div>
                            <h4 className="text-xs font-bold text-emerald-100">조회 결과</h4>
                        </div>
                        
                        <div className="py-2">
                            <p className="text-2xl font-black tracking-tight text-white">수취인 확인 완료</p>
                        </div>

                        <div className="pt-3 border-t border-white/10">
                            <span className="text-[9px] font-black text-emerald-200 uppercase tracking-widest">실시간 정보가 확인되었습니다.</span>
                        </div>
                    </Card>

                    <div className="space-y-3">
                        <Button
                            onClick={nextStep}
                            variant="primary"
                            size="xl"
                            fullWidth
                            className="h-16 rounded-2xl text-lg font-black shadow-lg bg-slate-900 text-white hover:bg-slate-800 transition-all group"
                        >
                            확인 완료
                            <ArrowRight className="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" />
                        </Button>
                        <Button
                            onClick={prevStep}
                            variant="outline"
                            size="lg"
                            fullWidth
                            className="h-12 rounded-xl text-sm font-bold border-slate-200 text-slate-500 hover:bg-slate-50"
                        >
                            정보 다시 입력
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RecipientConfirmForm;
