import React, { useState } from 'react';
import { registerEmployee } from '../../../api/employee';
import type { RegisterEmployeeRequest } from '../../../types/employee';

interface Props {
    onSuccess: () => void;
    onCancel: () => void;
}

const EmployeeRegistrationForm: React.FC<Props> = ({ onSuccess, onCancel }) => {
    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<'USER' | 'ADMIN'>('USER');
    const [agencyId, setAgencyId] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setIsLoading(true);

        const employeeData: RegisterEmployeeRequest = {
            loginId,
            password,
            role,
            agencyId: parseInt(agencyId, 10),
        };

        try {
            await registerEmployee(employeeData);
            onSuccess();
        } catch (err: any) {
            setError(err.message || '직원 등록에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-6">
            <div>
                <label htmlFor="loginId" className="block text-sm font-medium text-gray-700">
                    사번 (Login ID)
                </label>
                <input
                    type="text"
                    id="loginId"
                    value={loginId}
                    onChange={(e) => setLoginId(e.target.value)}
                    required
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
                />
            </div>
            <div>
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
            </div>
            <div>
                <label htmlFor="role" className="block text-sm font-medium text-gray-700">
                    권한
                </label>
                <select
                    id="role"
                    value={role}
                    onChange={(e) => setRole(e.target.value as 'USER' | 'ADMIN')}
                    className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm rounded-md"
                >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                </select>
            </div>
            <div>
                <label htmlFor="agencyId" className="block text-sm font-medium text-gray-700">
                    소속 대행업체 ID
                </label>
                <input
                    type="number"
                    id="agencyId"
                    value={agencyId}
                    onChange={(e) => setAgencyId(e.target.value)}
                    required
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
                />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <div className="flex justify-end gap-3">
                <button
                    type="button"
                    onClick={onCancel}
                    className="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500"
                >
                    취소
                </button>
                <button
                    type="submit"
                    disabled={isLoading}
                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {isLoading ? '등록 중...' : '직원 등록'}
                </button>
            </div>
        </form>
    );
};

export default EmployeeRegistrationForm;
