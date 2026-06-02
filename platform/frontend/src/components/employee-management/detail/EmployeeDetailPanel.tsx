import React, { useState } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { User, ShieldCheck, Lock, Unlock } from 'lucide-react';
import Modal from '../../common/Modal';
import DeleteEmployeeForm from '../actions/DeleteEmployeeForm';
import ResetPasswordForm from '../actions/ResetPasswordForm';

const EmployeeDetailPanel: React.FC = () => {
    const { selectedEmployee } = useEmployeeStore();
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [isResetModalOpen, setIsResetModalOpen] = useState(false);
    const [showResetSuccess, setShowResetSuccess] = useState(false);

    if (!selectedEmployee) {
        return (
            <div className="h-full flex items-center justify-center text-gray-400">
                직원을 선택해주세요.
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col items-center gap-4 bg-gray-50 rounded-xl p-6 border border-gray-100">
                <div className="w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center text-blue-600">
                    <User className="w-8 h-8" />
                </div>
                <div className="text-center">
                    <div className="text-xl font-bold text-gray-900">{selectedEmployee.loginId}</div>
                    <div className="text-sm text-gray-500">사번: {selectedEmployee.employeeNum}</div>
                </div>
            </div>

            <div className="space-y-4">
                <div className="flex items-center gap-2 text-sm text-gray-700">
                    <User className="w-4 h-4 text-gray-500" />
                    <span className="font-medium">권한:</span>
                    <span className="font-bold">{selectedEmployee.role === 'ADMIN' ? '관리자' : '일반 직원'}</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-700">
                    <ShieldCheck className="w-4 h-4 text-gray-500" />
                    <span className="font-medium">소속 대행업체 ID:</span>
                    <span className="font-bold">{selectedEmployee.agencyId}</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-700">
                    {selectedEmployee.isLocked ? (
                        <Lock className="w-4 h-4 text-red-500" />
                    ) : (
                        <Unlock className="w-4 h-4 text-emerald-500" />
                    )}
                    <span className="font-medium">계정 상태:</span>
                    <span className={`font-bold ${selectedEmployee.isLocked ? 'text-red-600' : 'text-emerald-600'}`}>
                        {selectedEmployee.isLocked ? '잠김' : '정상'}
                    </span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-700">
                    {selectedEmployee.isDeleted ? (
                        <span className="px-2 py-0.5 text-xs font-semibold text-red-700 bg-red-100 rounded-full">삭제됨</span>
                    ) : (
                        <span className="px-2 py-0.5 text-xs font-semibold text-emerald-700 bg-emerald-100 rounded-full">활성</span>
                    )}
                </div>
                <div className="text-sm text-gray-500">
                    <span className="font-medium">등록일시:</span> {new Date(selectedEmployee.createdAt).toLocaleString()}
                </div>
            </div>

            <div className="border-t border-gray-100 pt-6 mt-6 space-y-4">
                <button
                    onClick={() => setIsDeleteModalOpen(true)}
                    className="w-full py-3 bg-red-500 text-white rounded-xl font-bold hover:bg-red-600 transition-colors"
                >
                    직원 삭제
                </button>
                <button
                    onClick={() => setIsResetModalOpen(true)}
                    className="w-full py-3 bg-blue-500 text-white rounded-xl font-bold hover:bg-blue-600 transition-colors"
                >
                    비밀번호 초기화
                </button>
            </div>

            {selectedEmployee && (
                <>
                    <Modal
                        isOpen={isDeleteModalOpen}
                        onClose={() => setIsDeleteModalOpen(false)}
                        title="직원 삭제 확인"
                        maxWidth="sm"
                    >
                        <DeleteEmployeeForm
                            loginId={selectedEmployee.loginId}
                            onSuccess={() => setIsDeleteModalOpen(false)}
                            onCancel={() => setIsDeleteModalOpen(false)}
                        />
                    </Modal>

                    <Modal
                        isOpen={isResetModalOpen}
                        onClose={() => setIsResetModalOpen(false)}
                        title="비밀번호 초기화 확인"
                        maxWidth="sm"
                    >
                        <ResetPasswordForm
                            loginId={selectedEmployee.loginId}
                            onSuccess={() => {
                                setShowResetSuccess(true);
                                setIsResetModalOpen(false);
                            }}
                            onCancel={() => setIsResetModalOpen(false)}
                        />
                    </Modal>

                    <Modal
                        isOpen={showResetSuccess}
                        onClose={() => setShowResetSuccess(false)}
                        title="비밀번호 초기화 완료"
                        maxWidth="sm"
                    >
                        <div className="space-y-4">
                            <p className="text-gray-700">
                                직원 <span className="font-bold">{selectedEmployee.loginId}</span>의 비밀번호가 성공적으로 변경되었습니다.
                            </p>
                            <div className="flex justify-end">
                                <button
                                    onClick={() => setShowResetSuccess(false)}
                                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500"
                                >
                                    확인
                                </button>
                            </div>
                        </div>
                    </Modal>
                </>
            )}
        </div>
    );
};

export default EmployeeDetailPanel;
