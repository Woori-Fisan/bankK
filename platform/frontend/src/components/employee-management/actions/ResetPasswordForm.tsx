import React, { useState } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { resetEmployeePassword } from '../../../api/employee';

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
        <div>
            <p className="text-gray-700 mb-6">
                직원 <span className="font-bold">{loginId}</span>의 비밀번호를 초기화하시겠습니까? 변경할 비밀번호를 입력해 주세요.
            </p>

            <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                    초기 비밀번호
                </label>
                <input
                    type="password"
                    id="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
                />

            {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

            <div className="flex justify-end gap-3">
                <button
                    type="button"
                    onClick={onCancel}
                    className="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-all"
                >
                    취소
                </button>
                <button
                    type="button"
                    onClick={handleReset}
                    disabled={isLoading}
                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                >
                    {isLoading ? '초기화 중...' : '초기화'}
                </button>
            </div>
        </div>
    );
};

export default ResetPasswordForm;
