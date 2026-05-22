import React, { useState } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { deleteEmployee } from '../../../api/employee';

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
        <div>
            <p className="text-gray-700 mb-6 leading-relaxed">
                정말로 직원 <span className="font-bold text-slate-900">{loginId}</span> 계정을 삭제하시겠습니까? <br/>
                <span className="text-rose-500 text-sm font-medium">이 작업은 되돌릴 수 없습니다.</span>
            </p>

            {error && <p className="text-red-500 text-sm mb-4 font-bold">{error}</p>}

            <div className="flex justify-end gap-3">
                <button
                    type="button"
                    onClick={onCancel}
                    className="inline-flex justify-center py-2.5 px-5 border border-gray-300 shadow-sm text-sm font-bold rounded-xl text-gray-700 bg-white hover:bg-gray-50 focus:outline-none transition-all"
                >
                    취소
                </button>
                <button
                    type="button"
                    onClick={handleDelete}
                    disabled={isLoading}
                    className="inline-flex justify-center py-2.5 px-5 border border-transparent shadow-sm text-sm font-bold rounded-xl text-white bg-rose-600 hover:bg-rose-700 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                >
                    {isLoading ? '삭제 중...' : '계정 삭제'}
                </button>
            </div>
        </div>
    );
};

export default DeleteEmployeeForm;
