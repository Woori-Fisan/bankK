import React, { useState } from 'react';
import EmployeeList from '../components/employee-management/list/EmployeeList';
import EmployeeDetailPanel from '../components/employee-management/detail/EmployeeDetailPanel';
import EmployeeRegistrationForm from '../components/employee-management/forms/EmployeeRegistrationForm';
import Modal from '../components/common/Modal';
import { useEmployeeStore } from '../store/useEmployeeStore';
import PageHeader from '../components/common/PageHeader';
import { Button } from '../components/common/Button';
import Card from '../components/common/Card';
import Section from '../components/common/Section';

const EmployeeManagementPage: React.FC = () => {
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
    const { setPage, setSelectedEmployee, triggerRefresh } = useEmployeeStore();

    const handleRegistrationSuccess = () => {
        setIsRegisterModalOpen(false);
        setPage(0);
        triggerRefresh();
        setSelectedEmployee(null);
    };

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50">
            <div className="max-w-7xl mx-auto px-10 py-12 w-full min-h-full flex flex-col">
                <PageHeader 
                title="직원 관리"
                description="직원 계정 생성, 조회, 수정 및 삭제 요청을 관리합니다."
                action={
                    <Button
                        onClick={() => setIsRegisterModalOpen(true)}
                        variant="emerald"
                    >
                        + 새 직원 등록
                    </Button>
                }
            />

            <Section columns={3} gap={6}>
                {/* Left Panel: Employee List (2/3 width) */}
                <Card padding="lg" className="col-span-1 md:col-span-2 flex flex-col">
                    <h2 className="text-xl font-bold text-slate-900 mb-6">직원 목록</h2>
                    <EmployeeList />
                </Card>

                {/* Right Panel: Employee Details (1/3 width) */}
                <Card padding="lg" className="col-span-1 flex flex-col">
                    <h2 className="text-xl font-bold text-slate-900 mb-6">직원 상세 정보</h2>
                    <EmployeeDetailPanel />
                </Card>
            </Section>

            <Modal
                isOpen={isRegisterModalOpen}
                onClose={() => setIsRegisterModalOpen(false)}
                title="새 직원 등록"
                maxWidth="3xl"
            >
                <EmployeeRegistrationForm 
                    onSuccess={handleRegistrationSuccess} 
                    onCancel={() => setIsRegisterModalOpen(false)} 
                />
            </Modal>
        </div>
    </div>
);
};

export default EmployeeManagementPage;
