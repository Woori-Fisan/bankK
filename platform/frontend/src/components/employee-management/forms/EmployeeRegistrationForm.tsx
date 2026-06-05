import React, { useState } from 'react';
import { registerEmployee } from '../../../api/employee';
import type { RegisterEmployeeRequest } from '../../../types/employee';
import Input from '../../common/Input';
import { Button } from '../../common/Button';
import { User, ShieldCheck, Lock, Hash } from 'lucide-react';

interface Props {
    onSuccess: () => void;
    onCancel: () => void;
}

const EmployeeRegistrationForm: React.FC<Props> = ({ onSuccess, onCancel }) => {
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [agencyId, setAgencyId] = useState('');
    const [employeeNum, setEmployeeNum] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async () => {
        if (!loginId || !password || !agencyId || !employeeNum) {
            setError('모든 필수 항목을 입력해주세요.');
            return;
        }

        setError(null);
        setIsLoading(true);

        const employeeData: RegisterEmployeeRequest = {
            loginId,
            password,
            role: 'USER',
            agencyId: parseInt(agencyId, 10),
            employeeNum,
        };

        try {
            await registerEmployee(employeeData);
            onSuccess();
        } catch (err: any) {
            const errorMessage = 
                err.response?.data?.error?.message || 
                err.response?.data?.message || 
                err.message || 
                '직원 등록에 실패했습니다.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                    label="Login ID"
                    icon={User}
                    placeholder="사용자 아이디 입력"
                    value={loginId}
                    onChange={(e) => setLoginId(e.target.value)}
                    required
                />
                <Input
                    label="사번 (Employee Num)"
                    icon={Hash}
                    placeholder="사원 번호 입력"
                    value={employeeNum}
                    onChange={(e) => setEmployeeNum(e.target.value)}
                    required
                />
            </div>

            <Input
                type="password"
                label="초기 비밀번호"
                icon={Lock}
                placeholder="최초 접속용 비밀번호"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                    label="권한"
                    icon={ShieldCheck}
                    value="USER (일반 직원)"
                    readOnly
                    disabled
                />
                <Input
                    type="number"
                    label="소속 대행업체 ID"
                    icon={ShieldCheck}
                    placeholder="대행기관 ID 번호"
                    value={agencyId}
                    onChange={(e) => setAgencyId(e.target.value)}
                    required
                />
            </div>

            {error && <p className="text-rose-500 text-sm font-bold ml-1">{error}</p>}

            <div className="flex justify-end gap-3 pt-4 border-t border-slate-50">
                <Button
                    variant="outline"
                    onClick={onCancel}
                    disabled={isLoading}
                    className="px-6"
                >
                    취소
                </Button>
                <Button
                    variant="emerald"
                    onClick={handleSubmit}
                    disabled={isLoading}
                    className="px-10"
                >
                    {isLoading ? '등록 중...' : '직원 계정 생성'}
                </Button>
            </div>
        </div>
    );
};

export default EmployeeRegistrationForm;
