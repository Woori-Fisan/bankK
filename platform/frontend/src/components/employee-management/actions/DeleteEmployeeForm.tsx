import React, { useState } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { deleteEmployee } from '../../../api/employee';
import { Button } from '../../common/Button';

interface Props {
    loginId: string;
    onSuccess: () => void;
    onCancel: () => void;
}

const DeleteEmployeeForm: React.FC<Props> = ({ loginId, onSuccess, onCancel }) => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const { setPage, setSelectedEmployee, triggerRefresh } = useEmployeeStore();

    const handleDelete = async () => {
        setError(null);
        setIsLoading(true);
        try {
            await deleteEmployee(loginId);
            triggerRefresh(); // 목록 새로고침 트리거
            onSuccess();
            setPage(0);
            setSelectedEmployee(null);
        } catch (err: any) {
            const errorMessage = 
                err.response?.data?.error?.message || 
                err.response?.data?.message || 
                err.message || 
                '직원 삭제에 실패했습니다.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="space-y-8">
            <div className="space-y-4">
                <p className="text-slate-600 leading-relaxed">
                    정말로 직원 <span className="font-bold text-slate-900">{loginId}</span> 계정을 삭제하시겠습니까?
                </p>
                <div className="p-4 bg-rose-50 border border-rose-100 rounded-2xl">
                    <p className="text-rose-600 text-sm font-bold">
                        ⚠️ 이 작업은 되돌릴 수 없으며, 해당 직원은 즉시 시스템 접근이 차단됩니다.
                    </p>
                </div>
            </div>

            {error && <p className="text-rose-500 text-sm font-bold ml-1">{error}</p>}

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
                    variant="danger"
                    onClick={handleDelete}
                    disabled={isLoading}
                    className="px-8"
                >
                    {isLoading ? '삭제 중...' : '직원 계정 삭제'}
                </Button>
            </div>
        </div>
    );
};

export default DeleteEmployeeForm;
