import React, { useEffect } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { fetchEmployees } from '../../../api/employee';
import EmployeeListItem from './EmployeeListItem';
import EmployeePagination from './EmployeePagination';

const EmployeeList: React.FC = () => {
    const { employees, isLoading, error, filters, refreshKey, setEmployees, setLoading, setError } = useEmployeeStore();

    useEffect(() => {
        const loadEmployees = async () => {
            setLoading(true);
            try {
                const response = await fetchEmployees(filters.page, filters.size, filters.agencyId);
                
                if (!response.data) {
                    throw new Error('서버에서 직원 목록 데이터를 받지 못했습니다.');
                }

                console.log(response);
                
                setEmployees(response.data);
                setError(null);
            } catch (err: any) {
                const errorMessage = 
                    err.response?.data?.error?.message || 
                    err.response?.data?.message || 
                    err.message || 
                    '직원 목록을 불러오는데 실패했습니다.';
                setError(errorMessage);
            } finally {
                setLoading(false);
            }
        };

        loadEmployees();
    }, [filters.page, filters.size, filters.agencyId, refreshKey]); // refreshKey 추가

    if (isLoading) {
        return <div className="p-4 text-center text-gray-500">로딩 중...</div>;
    }

    if (error) {
        return <div className="p-4 text-center text-red-500">오류: {error}</div>;
    }

    return (
        <div className="flex flex-col h-full">
            {employees.length === 0 ? (
                <div className="p-4 text-center text-gray-500">등록된 직원이 없습니다.</div>
            ) : (
                <div className="overflow-y-auto pr-2 space-y-4 max-h-[500px]">
                    {employees.map(employee => (
                        <EmployeeListItem key={employee.loginId} employee={employee} />
                    ))}
                </div>
            )}
            <div className="mt-4 pt-4 border-t border-gray-100">
                <EmployeePagination />
            </div>
        </div>
    );
};

export default EmployeeList;