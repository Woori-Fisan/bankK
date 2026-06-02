import React from 'react';
import type { Employee } from '../../../types/employee';
import { useEmployeeStore } from '../../../store/useEmployeeStore';

interface Props {
    employee: Employee;
}

const EmployeeListItem: React.FC<Props> = ({ employee }) => {
    const { selectedEmployee, setSelectedEmployee } = useEmployeeStore();
    const isSelected = selectedEmployee?.loginId === employee.loginId;
    const isDeleted = employee.isDeleted;

    return (
        <div
            className={`flex justify-between items-center p-4 rounded-xl transition-colors border ${
                isDeleted 
                    ? 'bg-gray-50 border-gray-200 opacity-60 cursor-not-allowed' 
                    : isSelected 
                        ? 'bg-emerald-50 border-emerald-300 cursor-pointer' 
                        : 'bg-white hover:bg-gray-50 border-gray-100 cursor-pointer'
            }`}
            onClick={() => !isDeleted && setSelectedEmployee(employee)}
        >
            <div className="flex items-center gap-3">
                <div>
                    <div className={`font-bold ${isDeleted ? 'text-gray-400' : 'text-gray-900'}`}>
                        {employee.loginId}
                        {isDeleted && (
                            <span className="ml-2 px-2 py-0.5 text-[10px] font-bold text-red-600 bg-red-50 border border-red-100 rounded-md uppercase">
                                삭제됨
                            </span>
                        )}
                    </div>
                    <div className={`text-sm ${isDeleted ? 'text-gray-400' : 'text-gray-500'}`}>
                        사번: {employee.employeeNum} &bull; 업체: {employee.agencyId} &bull; {employee.role}
                    </div>
                </div>
            </div>
            {!isDeleted && (
                <button
                    className={`text-sm font-bold ${isSelected ? 'text-emerald-700' : 'text-gray-500 hover:text-emerald-600'} transition-colors`}
                    onClick={(e) => {
                        e.stopPropagation();
                        setSelectedEmployee(employee);
                    }}
                >
                    Review Details
                </button>
            )}
        </div>
    );
};

export default EmployeeListItem;
