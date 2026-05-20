import React, { useState } from 'react';
import { Search, CreditCard, User, Landmark, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';
import { isValidAccountNumber } from '../utils/validator';

interface AccountInputStepProps {
    onNext: (data: { 
        bankCode: string; 
        accountNo: string; 
        customerRrnPrefix: string;
        encryptedKey: string;
        jwsSignature: string;
    }) => void;
}

const AccountInputStep: React.FC<AccountInputStepProps> = ({ onNext }) => {
    const [formData, setFormData] = useState({
        bankCode: '',
        accountNo: '',
        customerRrnPrefix: '', // 백엔드 DTO 규격 준수
        // 사용자 요청에 따른 테스트용 하드코딩 문자열 설정
        encryptedKey: 'ENC_AES_KEY_STRING',
        jwsSignature: 'JWS_SIGNATURE_STRING',
    });
    const [error, setError] = useState('');

    const banks = [
        { code: '020', name: '우리은행' },
        { code: '081', name: '하나은행' },
        { code: '088', name: '신한은행' },
        { code: '004', name: 'KB국민은행' },
        { code: '011', name: 'NH농협은행' },
        { code: '003', name: 'IBK기업은행' },
        { code: '071', name: '우체국' },
    ];

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        setError(''); // 입력 시 에러 초기화
    };

    const handleSubmit = () => {
        // 1. 유효성 검사 (RULE_FE_STYLE 9. 유효성 검사 준수)
        if (!formData.bankCode) {
            setError('은행을 선택해 주세요.');
            return;
        }
        if (!isValidAccountNumber(formData.accountNo)) {
            setError('유효한 계좌번호를 입력해 주세요. (10~14자리 숫자)');
            return;
        }
        if (!/^\d{6}-[1-4]$/.test(formData.customerRrnPrefix)) {
            setError('주민번호 형식이 올바르지 않습니다. (예: 900101-1)');
            return;
        }

        // 2. 백엔드 DTO 규격에 맞는 완성된 객체 전달
        onNext(formData);
    };

    return (
        <div className="flex-1 flex flex-col items-center justify-center p-6 bg-slate-50/50">
            <div className="w-full max-w-lg">
                <header className="mb-10 text-center">
                    <div className="inline-flex items-center justify-center w-20 h-20 bg-emerald-100 rounded-3xl mb-6 shadow-sm">
                        <Search className="w-10 h-10 text-emerald-600" />
                    </div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">계좌 정보 입력</h1>
                    <p className="text-slate-500 mt-3 text-lg">조회하실 계좌의 정보를 정확히 입력해 주세요.</p>
                </header>

                <div className="bg-white rounded-3xl shadow-xl shadow-slate-200/50 p-10 border border-slate-100 transition-all hover:shadow-2xl hover:shadow-slate-200/60">
                    <div className="space-y-6">
                        {/* 은행 선택 */}
                        <div className="relative">
                            <label className="block text-sm font-bold text-slate-700 mb-2.5 flex items-center gap-2">
                                <Landmark className="w-4 h-4 text-emerald-500" />
                                은행 선택
                            </label>
                            <div className="relative">
                                <select
                                    name="bankCode"
                                    value={formData.bankCode}
                                    onChange={handleChange}
                                    className="w-full px-5 py-4 rounded-2xl border-2 border-slate-100 bg-slate-50 text-slate-900 focus:border-emerald-500 focus:bg-white outline-none transition-all appearance-none cursor-pointer pr-12 font-medium"
                                >
                                    <option value="" className="text-slate-400">은행을 선택하세요</option>
                                    {banks.map(bank => (
                                        <option key={bank.code} value={bank.code}>
                                            {bank.name}
                                        </option>
                                    ))}
                                </select>
                                <div className="absolute right-5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </div>
                            </div>
                        </div>

                        {/* 계좌 번호 */}
                        <div>
                            <label className="block text-sm font-bold text-slate-700 mb-2.5 flex items-center gap-2">
                                <CreditCard className="w-4 h-4 text-emerald-500" />
                                계좌번호
                            </label>
                            <input
                                type="text"
                                name="accountNo"
                                value={formData.accountNo}
                                onChange={handleChange}
                                placeholder="'-' 없이 숫자만 입력"
                                className="w-full px-5 py-4 rounded-2xl border-2 border-slate-100 bg-slate-50 text-slate-900 focus:border-emerald-500 focus:bg-white outline-none transition-all font-medium placeholder:text-slate-400"
                            />
                        </div>

                        {/* 주민번호 앞자리 */}
                        <div>
                            <label className="block text-sm font-bold text-slate-700 mb-2.5 flex items-center gap-2">
                                <User className="w-4 h-4 text-emerald-500" />
                                주민번호 앞 7자리
                            </label>
                            <input
                                type="text"
                                name="customerRrnPrefix"
                                value={formData.customerRrnPrefix}
                                onChange={handleChange}
                                placeholder="예: 900101-1"
                                maxLength={8}
                                className="w-full px-5 py-4 rounded-2xl border-2 border-slate-100 bg-slate-50 text-slate-900 focus:border-emerald-500 focus:bg-white outline-none transition-all font-medium placeholder:text-slate-400"
                            />
                        </div>

                        {/* 테스트용 보안 토큰 표시 (개발용) */}
                        <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 flex items-start gap-3">
                            <ShieldCheck className="w-5 h-5 text-slate-400 flex-shrink-0 mt-0.5" />
                            <div className="flex-1 min-w-0">
                                <p className="text-xs text-slate-500 font-bold mb-1">Security Payload (Test Mode)</p>
                                <p className="text-[10px] text-slate-400 font-mono truncate">KEY: {formData.encryptedKey}</p>
                                <p className="text-[10px] text-slate-400 font-mono truncate">SIG: {formData.jwsSignature}</p>
                            </div>
                        </div>

                        {/* 에러 메시지 */}
                        {error && (
                            <div className="flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100">
                                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                                <span className="text-sm font-bold">{error}</span>
                            </div>
                        )}

                        {/* 안내 문구 */}
                        <div className="flex items-start gap-3.5 p-5 bg-slate-50 rounded-2xl border border-slate-100">
                            <AlertCircle className="w-5 h-5 text-slate-400 flex-shrink-0 mt-0.5" />
                            <p className="text-sm text-slate-500 leading-relaxed font-medium">
                                입력하신 정보는 본인 확인 및 계좌 조회를 위해 해당 금융기관으로 안전하게 전송됩니다. 
                                중계 플랫폼에는 고객님의 개인정보를 별도로 저장하지 않습니다.
                            </p>
                        </div>

                        {/* 조회 버튼 */}
                        <button
                            type="button"
                            onClick={handleSubmit}
                            className="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-5 rounded-2xl shadow-xl shadow-slate-200 transition-all transform hover:-translate-y-1 flex items-center justify-center gap-2 group"
                        >
                            계좌 조회하기
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AccountInputStep;
