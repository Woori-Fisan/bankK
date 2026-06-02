import { AlertCircle, ArrowRight, Search, Info } from 'lucide-react';
import { isValidAccountNumber } from '../../utils/validator';
import { fetchBankList, type BankOption } from '../../api/loanApi';
import { Button } from '../common/Button';
import Card from '../common/Card';
import PageHeader from '../common/PageHeader';
import RrnInput from '../common/RrnInput';
import AccountInputSection from '../common/AccountInputSection';
import { useEffect, useState } from 'react';

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
        bankName: '',
        accountNo: '',
        rrnFront: '',
        rrnBack: '',
        encryptedKey: 'ENC_AES_KEY_STRING',
        jwsSignature: 'JWS_SIGNATURE_STRING',
    });
    
    const [banks, setBanks] = useState<BankOption[]>([]);
    const [fieldErrors, setFieldErrors] = useState({
        bankCode: '',
        accountNo: '',
        rrn: ''
    });

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

    const handleChange = (name: string, value: string) => {
        setFormData(prev => ({ ...prev, [name]: value }));
        setFieldErrors(prev => ({ ...prev, [name === 'accountNo' ? 'accountNo' : name]: '', rrn: name.startsWith('rrn') ? '' : prev.rrn }));
        if (clearApiError) clearApiError();
    };

    const handleRrnChange = (name: 'rrnFront' | 'rrnBack', value: string) => {
        setFormData(prev => ({ ...prev, [name]: value }));
        setFieldErrors(prev => ({ ...prev, rrn: '' }));
        if (clearApiError) clearApiError();
    };

    const handleBlur = () => {
        // Validation logic if needed on blur
    };

    const handleSubmit = () => {
        const newErrors = {
            bankCode: formData.bankCode ? '' : '은행을 선택해 주세요.',
            accountNo: isValidAccountNumber(formData.accountNo) ? '' : '유효한 계좌번호를 입력해 주세요. (10~14자리 숫자)',
            rrn: (formData.rrnFront.length === 6 && formData.rrnBack.length === 1) ? '' : '주민번호 정보를 모두 입력해 주세요.'
        };

        const hasError = Object.values(newErrors).some(error => error !== '');
        if (hasError) {
            setFieldErrors(newErrors);
            return;
        }

        onNext({
            bankCode: formData.bankCode,
            accountNo: formData.accountNo,
            customerRrnPrefix: formData.rrnFront + formData.rrnBack,
            encryptedKey: formData.encryptedKey,
            jwsSignature: formData.jwsSignature
        });
    };

    const isNextDisabled = !formData.bankCode || !formData.accountNo || formData.rrnFront.length !== 6 || formData.rrnBack.length !== 1;

    return (
        <div className="flex-1 overflow-auto bg-slate-50/50 px-10 py-12">
            <div className="max-w-7xl mx-auto w-full">
                <PageHeader 
                    title="계좌 정보 입력"
                    description="조회하실 계좌의 정보를 정확히 입력해 주세요."
                />

                {apiError && (
                    <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 max-w-4xl mx-auto">
                        <AlertCircle className="w-5 h-5 flex-shrink-0" />
                        <span className="text-sm font-bold">{apiError}</span>
                    </div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* 1. 입력 영역 (2컬럼) */}
                    <div className="lg:col-span-2 space-y-8">
                        <Card padding="lg" className="border-slate-100 shadow-sm">
                            <div className="space-y-10">
                                <AccountInputSection 
                                    title="조회 계좌 정보"
                                    bankCode={formData.bankCode}
                                    accountNumber={formData.accountNo}
                                    banks={banks}
                                    onBankChange={(name, code) => {
                                        setFormData(prev => ({ ...prev, bankName: name, bankCode: code }));
                                        setFieldErrors(prev => ({ ...prev, bankCode: '' }));
                                    }}
                                    onAccountChange={(val) => {
                                        setFormData(prev => ({ ...prev, accountNo: val }));
                                        setFieldErrors(prev => ({ ...prev, accountNo: '' }));
                                    }}
                                    error={{
                                        bankCode: fieldErrors.bankCode,
                                        accountNumber: fieldErrors.accountNo
                                    }}
                                />

                                <div className="pt-8 border-t border-slate-50">
                                    <RrnInput 
                                        rrnFront={formData.rrnFront}
                                        rrnBack={formData.rrnBack}
                                        onRrnFrontChange={(val) => handleRrnChange('rrnFront', val)}
                                        onRrnBackChange={(val) => handleRrnChange('rrnBack', val)}
                                        onBlur={handleBlur}
                                        error={fieldErrors.rrn}
                                    />
                                </div>
                            </div>
                        </Card>

                        <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4">
                            <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                            <p className="text-xs text-slate-500 leading-relaxed font-medium">
                                입력하신 정보는 본인 확인 및 계좌 조회를 위해 해당 금융기관으로 안전하게 전송됩니다. 
                                중계 플랫폼에는 고객님의 개인정보를 별도로 저장하지 않습니다.
                            </p>
                        </div>
                    </div>

                    {/* 2. 요약 및 실행 영역 (1컬럼) */}
                    <div className="lg:col-span-1 space-y-6">
                        <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[320px]">
                            <div className="space-y-6">
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">조회 요약</span>
                                    <Search className="w-4 h-4 text-slate-300" />
                                </div>
                                
                                <div className="space-y-4">
                                    <div className="space-y-1">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">대상 은행</p>
                                        <p className="text-sm font-black text-slate-900">{formData.bankName || '은행 미선택'}</p>
                                    </div>
                                    <div className="space-y-1">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">계좌 번호</p>
                                        <p className="text-sm font-black text-slate-900 font-mono">{formData.accountNo || '번호 미입력'}</p>
                                    </div>
                                </div>
                            </div>

                            <Button
                                onClick={handleSubmit}
                                disabled={isNextDisabled}
                                variant={isNextDisabled ? 'secondary' : 'primary'}
                                size="xl"
                                fullWidth
                                className={`h-16 rounded-2xl text-lg font-black shadow-lg transition-all group ${
                                    isNextDisabled ? 'bg-slate-200 text-slate-400' : 'bg-slate-900 text-white hover:bg-slate-800'
                                }`}
                            >
                                계좌 조회하기
                                <ArrowRight className={`w-5 h-5 ml-2 transition-transform ${isNextDisabled ? '' : 'group-hover:translate-x-1'}`} />
                            </Button>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AccountInputStep;

