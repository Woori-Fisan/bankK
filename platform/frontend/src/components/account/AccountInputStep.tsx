import { AlertCircle } from 'lucide-react';
import { isValidAccountNumber } from '../../utils/validator';
import { fetchBankList, type BankOption } from '../../api/loanApi';
import PageHeader from '../common/PageHeader';
import { useEffect, useState } from 'react';
import CommonConfirmModal from '../common/CommonConfirmModal';

// Sub-components
import AccountMainSection from './sections/AccountMainSection';
import AccountSideSummary from './sections/AccountSideSummary';

interface AccountInputStepProps {
    onNext: (data: { 
        bankCode: string; 
        accountNo: string;
        customerName: string,
        customerRrnPrefix: string;
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
            customerName: formData.userName
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
                    <div className="lg:col-span-2 space-y-8">
                        <AccountMainSection
                            formData={formData}
                            onNameChange={(val) => handleChange('userName', val)}
                            onRrnFrontChange={(val) => handleRrnChange('rrnFront', val)}
                            onRrnBackChange={(val) => handleRrnChange('rrnBack', val)}
                            onBankChange={(name, code) => {
                                setFormData(prev => ({ ...prev, bankName: name, bankCode: code }));
                                setFieldErrors(prev => ({ ...prev, bankCode: '' }));
                            }}
                            onAccountChange={(val) => {
                                setFormData(prev => ({ ...prev, accountNo: val }));
                                setFieldErrors(prev => ({ ...prev, accountNo: '' }));
                            }}
                            banks={banks}
                            fieldErrors={fieldErrors}
                        />
                    </div>

                    <div className="lg:col-span-1">
                        <AccountSideSummary
                            userName={formData.userName}
                            bankName={formData.bankName}
                            accountNo={formData.accountNo}
                            onSubmit={handleSubmit}
                            isNextDisabled={isNextDisabled}
                        />
                    </div>
                </div>
            </div>

            <CommonConfirmModal
                isOpen={isConfirmOpen}
                onClose={() => setIsConfirmOpen(false)}
                onConfirm={handleActualSubmit}
                title="조회 정보 확인"
                description="입력하신 내용이 정확한지 확인해 주세요."
                items={[
                    { label: '성명', value: formData.userName },
                    { 
                        label: '주민등록번호', 
                        value: (
                            <span className="font-mono tracking-widest">
                                {formData.rrnFront}-{formData.rrnBack}●●●●●●
                            </span>
                        ) 
                    },
                    { 
                        label: '대상 계좌', 
                        value: (
                            <div className="text-right">
                                <p className="text-sm font-bold text-emerald-600 font-mono">{formData.bankName}</p>
                                <p className="text-lg font-black text-slate-900">{formData.accountNo}</p>
                            </div>
                        )
                    }
                ]}
            />
        </div>
    );
};

export default AccountInputStep;
