import React, { useState, useEffect } from 'react';
import { Search, CreditCard, User, Landmark, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';
import { isValidAccountNumber } from '../../utils/validator';
import { fetchBankList, type BankOption } from '../../api/loanApi';

interface AccountInputStepProps {
    onNext: (data: { 
        bankCode: string; 
        accountNo: string; 
        customerRrnPrefix: string;
        encryptedKey: string;
        jwsSignature: string;
    }) => void;
    apiError?: string;
    clearApiError?: () => void;
}

const AccountInputStep: React.FC<AccountInputStepProps> = ({ onNext, apiError, clearApiError }) => {
    const [formData, setFormData] = useState({
        bankCode: '',
        accountNo: '',
        customerRrnPrefix: '', // 백엔드 DTO 규격 준수
        // 사용자 요청에 따른 테스트용 하드코딩 문자열 설정
        encryptedKey: 'ENC_AES_KEY_STRING',
        jwsSignature: 'JWS_SIGNATURE_STRING',
    });
    
    // 동적 은행 리스트 상태
    const [banks, setBanks] = useState<BankOption[]>([]);
    
    // 각 필드별 에러 상태
    const [fieldErrors, setFieldErrors] = useState({
        bankCode: '',
        accountNo: '',
        customerRrnPrefix: ''
    });

    // 컴포넌트 마운트 시 은행 리스트 조회
    useEffect(() => {
        const getBanks = async () => {
            try {
                const bankList = await fetchBankList();
                setBanks(bankList);
            } catch (error) {
                console.error('은행 리스트 조회 실패:', error);
            }
        };
        getBanks();
    }, []);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        
        let newValue = value;

        if (name === 'accountNo') {
            newValue = value.replace(/[^0-9]/g, '');
        } else if (name === 'customerRrnPrefix') {
            // 숫자만 추출 후 자동 하이픈 추가 (######-#)
            const numeric = value.replace(/[^0-9]/g, '');
            if (numeric.length <= 6) {
                newValue = numeric;
            } else {
                newValue = `${numeric.slice(0, 6)}-${numeric.slice(6, 7)}`;
            }
        }

        setFormData(prev => ({ ...prev, [name]: newValue }));
        
        // 입력 시 해당 필드의 에러 초기화
        setFieldErrors(prev => ({ ...prev, [name]: '' }));
        
        // 입력 시 상단의 API 에러 텍스트도 초기화
        if (clearApiError) {
            clearApiError();
        }
    };

    const handleBlur = (e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        let errorMessage = '';

        if (name === 'bankCode' && !value) {
            errorMessage = '은행을 선택해 주세요.';
        } else if (name === 'accountNo' && !isValidAccountNumber(value)) {
            errorMessage = '유효한 계좌번호를 입력해 주세요. (10~14자리 숫자)';
        } else if (name === 'customerRrnPrefix' && !/^\d{6}-[1-4]$/.test(value)) {
            errorMessage = '주민번호 형식이 올바르지 않습니다. (예: 900101-1)';
        }

        setFieldErrors(prev => ({ ...prev, [name]: errorMessage }));
    };

    const handleSubmit = () => {
        // 1. 유효성 검사 (전체 필드 검사)
        const newErrors = {
            bankCode: formData.bankCode ? '' : '은행을 선택해 주세요.',
            accountNo: isValidAccountNumber(formData.accountNo) ? '' : '유효한 계좌번호를 입력해 주세요. (10~14자리 숫자)',
            customerRrnPrefix: /^\d{6}-[1-4]$/.test(formData.customerRrnPrefix) ? '' : '주민번호 형식이 올바르지 않습니다. (예: 900101-1)'
        };

        const hasError = Object.values(newErrors).some(error => error !== '');

        if (hasError) {
            setFieldErrors(newErrors);
            return;
        }

        // 2. 백엔드 DTO 규격에 맞는 완성된 객체 전달
        onNext({
            ...formData,
            customerRrnPrefix: formData.customerRrnPrefix.replace(/-/g, '')
        });
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
                    
                    {/* 상단 API 에러 영역 (계좌를 찾을 수 없을 때 등) */}
                    {apiError && (
                        <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100">
                            <AlertCircle className="w-5 h-5 flex-shrink-0" />
                            <span className="text-sm font-bold">{apiError}</span>
                        </div>
                    )}

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
                                    onBlur={handleBlur}
                                    className={`w-full px-5 py-4 rounded-2xl border-2 bg-slate-50 text-slate-900 focus:bg-white outline-none transition-all appearance-none cursor-pointer pr-12 font-medium ${
                                        fieldErrors.bankCode ? 'border-rose-400 focus:border-rose-500' : 'border-slate-100 focus:border-emerald-500'
                                    }`}
                                >
                                    <option value="" className="text-slate-400">은행을 선택하세요</option>
                                    {banks.map(bank => (
                                        <option key={bank.bankCode} value={bank.bankCode}>
                                            {bank.bankName}
                                        </option>
                                    ))}
                                </select>
                                <div className="absolute right-5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </div>
                            </div>
                            {fieldErrors.bankCode && (
                                <p className="mt-2 text-sm text-rose-500 font-bold ml-1">{fieldErrors.bankCode}</p>
                            )}
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
                                onBlur={handleBlur}
                                onChange={handleChange}
                                placeholder="'-' 없이 숫자만 입력"
                                className={`w-full px-5 py-4 rounded-2xl border-2 bg-slate-50 text-slate-900 focus:bg-white outline-none transition-all font-medium placeholder:text-slate-400 ${
                                    fieldErrors.accountNo ? 'border-rose-400 focus:border-rose-500' : 'border-slate-100 focus:border-emerald-500'
                                }`}
                            />
                            <p className="mt-1.5 text-[11px] text-slate-400 font-medium ml-1">
                                * 계좌번호는 '-' 없이 숫자만 입력해 주세요.
                            </p>
                            {fieldErrors.accountNo && (
                                <p className="mt-2 text-sm text-rose-500 font-bold ml-1">{fieldErrors.accountNo}</p>
                            )}
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
                                onBlur={handleBlur}
                                onChange={handleChange}
                                placeholder="예: 900101-1"
                                maxLength={8}
                                className={`w-full px-5 py-4 rounded-2xl border-2 bg-slate-50 text-slate-900 focus:bg-white outline-none transition-all font-medium placeholder:text-slate-400 ${
                                    fieldErrors.customerRrnPrefix ? 'border-rose-400 focus:border-rose-500' : 'border-slate-100 focus:border-emerald-500'
                                }`}
                            />
                            {fieldErrors.customerRrnPrefix && (
                                <p className="mt-2 text-sm text-rose-500 font-bold ml-1">{fieldErrors.customerRrnPrefix}</p>
                            )}
                        </div>

                        {/* 테스트용 보안 토큰 표시 (개발용) */}
                        <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 flex items-start gap-3 mt-4">
                            <ShieldCheck className="w-5 h-5 text-slate-400 flex-shrink-0 mt-0.5" />
                            <div className="flex-1 min-w-0">
                                <p className="text-xs text-slate-500 font-bold mb-1">Security Payload (Test Mode)</p>
                                <p className="text-[10px] text-slate-400 font-mono truncate">KEY: {formData.encryptedKey}</p>
                                <p className="text-[10px] text-slate-400 font-mono truncate">SIG: {formData.jwsSignature}</p>
                            </div>
                        </div>

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
                            className="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-5 rounded-2xl shadow-xl shadow-slate-200 transition-all transform hover:-translate-y-1 flex items-center justify-center gap-2 group mt-2"
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
