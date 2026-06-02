import { AlertCircle, ArrowRight, Search, Info, User, AlertTriangle } from 'lucide-react';
import { isValidAccountNumber } from '../../utils/validator';
import { fetchBankList, type BankOption } from '../../api/loanApi';
import { Button } from '../common/Button';
import Card from '../common/Card';
import PageHeader from '../common/PageHeader';
import RrnInput from '../common/RrnInput';
import AccountInputSection from '../common/AccountInputSection';
import { useEffect, useState } from 'react';
import Input from '../common/Input';

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
        userName: '',
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
        userName: '',
        bankCode: '',
        accountNo: '',
        rrn: ''
    });
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);

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
            userName: formData.userName ? '' : '고객 성명을 입력해 주세요.',
            bankCode: formData.bankCode ? '' : '은행을 선택해 주세요.',
            accountNo: isValidAccountNumber(formData.accountNo) ? '' : '유효한 계좌번호를 입력해 주세요. (10~14자리 숫자)',
            rrn: (formData.rrnFront.length === 6 && formData.rrnBack.length === 1) ? '' : '주민번호 정보를 모두 입력해 주세요.'
        };

        const hasError = Object.values(newErrors).some(error => error !== '');
        if (hasError) {
            setFieldErrors(newErrors);
            return;
        }

        setIsConfirmOpen(true);
    };

    const handleActualSubmit = () => {
        setIsConfirmOpen(false);
        onNext({
            bankCode: formData.bankCode,
            accountNo: formData.accountNo,
            customerRrnPrefix: formData.rrnFront + formData.rrnBack,
            encryptedKey: formData.encryptedKey,
            jwsSignature: formData.jwsSignature
        });
    };

    const isNextDisabled = !formData.userName || !formData.bankCode || !formData.accountNo || formData.rrnFront.length !== 6 || formData.rrnBack.length !== 1;

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50">
            <div className="max-w-7xl mx-auto px-10 py-12 w-full min-h-full flex flex-col">
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
                                {/* 1. 고객 성명 */}
                                <div className="space-y-6">
                                    <div className="flex items-center gap-2 px-1">
                                        <User className="w-4 h-4 text-emerald-500" />
                                        <h3 className="text-sm font-bold text-slate-700">고객 정보</h3>
                                    </div>
                                    <div className="max-w-md">
                                        <Input
                                            label="고객 성명"
                                            placeholder="예) 홍길동"
                                            value={formData.userName}
                                            onChange={(e) => {
                                                setFormData(prev => ({ ...prev, userName: e.target.value }));
                                                setFieldErrors(prev => ({ ...prev, userName: '' }));
                                            }}
                                            error={fieldErrors.userName}
                                        />
                                    </div>
                                </div>

                                {/* 2. 주민등록번호 */}
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

                                {/* 3. 계좌 정보 */}
                                <div className="pt-8 border-t border-slate-50">
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
                        <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[420px]">
                            <div className="space-y-6">
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">조회 요약</span>
                                    <Search className="w-4 h-4 text-slate-300" />
                                </div>
                                
                                <div className="space-y-6">
                                    <div className="space-y-1">
                                        <p className="text-[10px] font-bold text-slate-400 uppercase">고객명</p>
                                        <p className="text-sm font-black text-slate-900">{formData.userName || '미입력'}</p>
                                    </div>
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

            {/* 최종 확인 모달 */}
            {isConfirmOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
                    <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                        {/* 헤더 */}
                        <div className="bg-emerald-600 px-10 py-8 flex items-center gap-6">
                            <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-md">
                                <AlertTriangle className="w-8 h-8 text-white" />
                            </div>
                            <div>
                                <h3 className="text-2xl font-black text-white">조회 정보 확인</h3>
                                <p className="text-emerald-100 mt-1 font-medium">
                                    입력하신 내용이 정확한지 확인해 주세요.
                                </p>
                            </div>
                        </div>

                        <div className="p-10 space-y-8">
                            <div className="bg-slate-50 rounded-3xl p-8 space-y-5 border border-slate-100">
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">성명</span>
                                    <span className="text-lg font-black text-slate-900">{formData.userName}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">주민등록번호</span>
                                    <span className="text-lg font-black text-slate-900 font-mono tracking-widest">
                                        {formData.rrnFront}-{formData.rrnBack}●●●●●●
                                    </span>
                                </div>
                                <div className="flex justify-between items-center pt-5 border-t border-slate-200/50">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">대상 계좌</span>
                                    <div className="text-right">
                                        <p className="text-lg font-black text-slate-900">{formData.bankName}</p>
                                        <p className="text-sm font-bold text-emerald-600 font-mono">{formData.accountNo}</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 버튼 */}
                        <div className="px-10 pb-10 grid grid-cols-2 gap-4">
                            <Button
                                onClick={() => setIsConfirmOpen(false)}
                                variant="secondary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200"
                            >
                                수정하기
                            </Button>
                            <Button
                                onClick={handleActualSubmit}
                                variant="primary"
                                size="xl"
                                className="h-16 rounded-2xl bg-slate-900 text-white hover:bg-slate-800 shadow-xl shadow-slate-900/20"
                            >
                                확인 완료
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AccountInputStep;

