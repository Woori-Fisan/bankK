import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { resetEmployeePassword } from '../../../api/employee';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    employeeId: string;
    onSuccess: (temporaryPassword: string) => void;
}

const ResetPasswordConfirmationModal: React.FC<Props> = ({ isOpen, onClose, employeeId, onSuccess }) => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleReset = async () => {
        setError(null);
        setIsLoading(true);
        try {
            const response = await resetEmployeePassword(employeeId);
            
            // 데이터가 존재하는지 안전하게 확인
            if (!response.data || !response.data.temporaryPassword) {
                throw new Error('서버에서 임시 비밀번호를 받지 못했습니다.');
            }
            
            onSuccess(response.data.temporaryPassword);
        } catch (err) {
            if (err instanceof Error) {
                setError(err.message || '비밀번호 초기화에 실패했습니다.');
            } else {
                setError('알 수 없는 오류로 비밀번호 초기화에 실패했습니다.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return ReactDOM.createPortal(
        <div className="fixed inset-0 flex justify-center items-center z-50">
            <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
                <h3 className="text-lg font-bold text-gray-900 mb-4">비밀번호 초기화 확인</h3>
                <p className="text-gray-700 mb-6">
                    직원 <span className="font-bold">{employeeId}</span>의 비밀번호를 초기화하시겠습니까? 임시 비밀번호가 발급됩니다.
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
                        onClick={handleReset}
                        disabled={isLoading}
                        className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isLoading ? '초기화 중...' : '초기화'}
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};

export default ResetPasswordConfirmationModal;
