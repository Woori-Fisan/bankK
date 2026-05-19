import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { deleteEmployee } from '../../../api/employee';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    employeeId: string;
    onSuccess: () => void;
}

const DeleteEmployeeConfirmationModal: React.FC<Props> = ({ isOpen, onClose, employeeId, onSuccess }) => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const { setPage, setSelectedEmployee } = useEmployeeStore();

    if (!isOpen) return null;

    const handleDelete = async () => {
        setError(null);
        setIsLoading(true);
        try {
            await deleteEmployee(employeeId);
            onSuccess();
            setPage(0);
            setSelectedEmployee(null);
        } catch (err: any) {
            setError(err.message || '직원 삭제에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return ReactDOM.createPortal(
        <div className="fixed inset-0 flex justify-center items-center z-50">
            <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
                <h3 className="text-lg font-bold text-gray-900 mb-4">직원 삭제 확인</h3>
                <p className="text-gray-700 mb-6">
                    정말로 직원 <span className="font-bold">{employeeId}</span> 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
                </p>

                {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

                <div className="flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        className="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500"
                    >
                        취소
                    </button>
                    <button
                        type="button"
                        onClick={handleDelete}
                        disabled={isLoading}
                        className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isLoading ? '삭제 중...' : '삭제'}
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};

export default DeleteEmployeeConfirmationModal;
