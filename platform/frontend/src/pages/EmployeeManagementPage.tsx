import React, { useState } from 'react';
import EmployeeList from '../components/employee-management/list/EmployeeList';
import EmployeeDetailPanel from '../components/employee-management/detail/EmployeeDetailPanel';
import EmployeeRegistrationForm from '../components/employee-management/forms/EmployeeRegistrationForm';
import Modal from '../components/common/Modal';
import { useEmployeeStore } from '../store/useEmployeeStore';

const EmployeeManagementPage: React.FC = () => {
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
    const { setPage, setSelectedEmployee, triggerRefresh } = useEmployeeStore();

    const handleRegistrationSuccess = () => {
        setIsRegisterModalOpen(false);
        setPage(0); // 첫 번째 페이지로 이동
        triggerRefresh(); // 현재 페이지가 0이더라도 강제로 데이터를 다시 불러옴
        setSelectedEmployee(null);
    };

    return (
        <div className="p-10 max-w-7xl mx-auto w-full">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">직원 관리</h1>
            <p className="text-gray-600 mb-6">
                직원 계정 생성, 조회, 수정 및 삭제 요청을 관리합니다.
            </p>

            <div className="flex justify-end mb-6">
                <button
                    onClick={() => setIsRegisterModalOpen(true)}
                    className="py-2 px-4 bg-emerald-600 text-white rounded-md shadow-sm hover:bg-emerald-700 transition-colors font-medium"
                >
                    + 새 직원 등록
                </button>
            </div>

            <div className="flex gap-6">
                {/* Left Panel: Employee List */}
                <div className="w-2/3 bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                    <h2 className="text-xl font-bold text-gray-900 mb-4">직원 목록</h2>
                    <EmployeeList />
                </div>

                {/* Right Panel: Employee Details / Actions */}
                <div className="w-1/3 bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                    <h2 className="text-xl font-bold text-gray-900 mb-4">직원 상세 정보</h2>
                    <EmployeeDetailPanel />
                </div>
            </div>

            <Modal
                isOpen={isRegisterModalOpen}
                onClose={() => setIsRegisterModalOpen(false)}
                title="새 직원 등록"
                maxWidth="md"
            >
                <EmployeeRegistrationForm 
                    onSuccess={handleRegistrationSuccess} 
                    onCancel={() => setIsRegisterModalOpen(false)} 
                />
            </Modal>
        </div>
    );
};

export default EmployeeManagementPage;
