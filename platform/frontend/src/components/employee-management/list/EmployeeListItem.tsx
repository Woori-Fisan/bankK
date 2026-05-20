import React from 'react';
import type { Employee } from '../../../types/employee';
import { useEmployeeStore } from '../../../store/useEmployeeStore';

interface Props {
    employee: Employee;
}

const EmployeeListItem: React.FC<Props> = ({ employee }) => {
    const { selectedEmployee, setSelectedEmployee } = useEmployeeStore();
    const isSelected = selectedEmployee?.employeeId === employee.employeeId;

    return (
        <div
            className={`flex justify-between items-center p-4 rounded-xl cursor-pointer transition-colors border ${
                isSelected ? 'bg-emerald-50 border-emerald-300' : 'bg-white hover:bg-gray-50 border-gray-100'
            }`}
            onClick={() => setSelectedEmployee(employee)}
        >
            <div>
                <div className="font-bold text-gray-900">{employee.employeeId}</div>
                <div className="text-sm text-gray-500">
                    {employee.agencyId} &bull; {employee.role}
                </div>
            </div>
            <button
                className={`text-sm font-bold ${isSelected ? 'text-emerald-700' : 'text-gray-500 hover:text-emerald-600'} transition-colors`}
                onClick={(e) => {
                    e.stopPropagation();
                    setSelectedEmployee(employee);
                }}
            >
                Review Details
            </button>
        </div>
    );
};

export default EmployeeListItem;
