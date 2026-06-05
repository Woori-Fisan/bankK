import React, { useState } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { resetEmployeePassword } from '../../../api/employee';
import Input from '../../common/Input';
import { Button } from '../../common/Button';
import { Lock } from 'lucide-react';

interface Props {
    loginId: string;
    onSuccess: () => void;
    onCancel: () => void;
}

const ResetPasswordForm: React.FC<Props> = ({ loginId, onSuccess, onCancel }) => {
    const { triggerRefresh } = useEmployeeStore();
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleReset = async () => {
        if (!password) {
            setError('새 비밀번호를 입력해주세요.');
            return;
        }

        setError(null);
        setIsLoading(true);
        try {
            await resetEmployeePassword(loginId, password);
            triggerRefresh(); // 목록 새로고침 트리거
            onSuccess();
        } catch (err: any) {
            const errorMessage = 
                err.response?.data?.error?.message || 
                err.response?.data?.message || 
                err.message || 
                '비밀번호 초기화에 실패했습니다.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="space-y-8">
            <p className="text-slate-600 leading-relaxed">
                직원 <span className="font-bold text-slate-900">{loginId}</span>의 비밀번호를 초기화하시겠습니까? <br/>
                변경할 새로운 비밀번호를 입력해 주세요.
            </p>

            <Input
                type="password"
                label="새 비밀번호"
                icon={Lock}
                placeholder="새로운 비밀번호 입력"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                error={error || undefined}
                required
            />

            <div className="flex justify-end gap-3 pt-2">
                <Button
                    variant="outline"
                    onClick={onCancel}
                    disabled={isLoading}
                    className="px-6"
                >
                    취소
                </Button>
                <Button
                    variant="primary"
                    onClick={handleReset}
                    disabled={isLoading}
                    className="px-8"
                >
                    {isLoading ? '초기화 중...' : '비밀번호 초기화'}
                </Button>
            </div>
        </div>
    );
};

export default ResetPasswordForm;
