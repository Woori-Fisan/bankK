import React, { useState } from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { User, ShieldCheck, Lock, Unlock } from 'lucide-react';
import Modal from '../../common/Modal';
import DeleteEmployeeForm from '../actions/DeleteEmployeeForm';
import ResetPasswordForm from '../actions/ResetPasswordForm';
import { Button } from '../../common/Button';
import Badge from '../../common/Badge';
import { formatDate } from '../../../utils/formatter';

const EmployeeDetailPanel: React.FC = () => {
    const { selectedEmployee } = useEmployeeStore();
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [isResetModalOpen, setIsResetModalOpen] = useState(false);
    const [showResetSuccess, setShowResetSuccess] = useState(false);

    if (!selectedEmployee) {
        return (
            <div className="h-full flex-1 flex items-center justify-center text-slate-400 font-medium">
                직원을 선택해주세요.
            </div>
        );
    }

    return (
        <div className="space-y-8 flex-1">
            <div className="flex flex-col items-center gap-4 bg-slate-50 rounded-2xl p-8 border border-slate-100">
                <div className="w-20 h-20 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 shadow-inner">
                    <User className="w-10 h-10" />
                </div>
                <div className="text-center">
                    <div className="text-2xl font-black text-slate-900">{selectedEmployee.loginId}</div>
                    <div className="text-sm text-slate-500 font-bold mt-1">사번: {selectedEmployee.employeeNum}</div>
                </div>
            </div>

            <div className="space-y-4 px-2">
                <div className="flex items-center justify-between py-3 border-b border-slate-50">
                    <div className="flex items-center gap-2 text-sm text-slate-500 font-bold">
                        <User className="w-4 h-4" />
                        권한
                    </div>
                    <Badge color={selectedEmployee.role === 'ADMIN' ? 'amber' : 'blue'}>
                        {selectedEmployee.role === 'ADMIN' ? '관리자' : '일반 직원'}
                    </Badge>
                </div>
                
                <div className="flex items-center justify-between py-3 border-b border-slate-50">
                    <div className="flex items-center gap-2 text-sm text-slate-500 font-bold">
                        <ShieldCheck className="w-4 h-4" />
                        소속 대행업체 ID
                    </div>
                    <span className="text-sm font-black text-slate-900">{selectedEmployee.agencyId}</span>
                </div>

                <div className="flex items-center justify-between py-3 border-b border-slate-50">
                    <div className="flex items-center gap-2 text-sm text-slate-500 font-bold">
                        {selectedEmployee.isLocked ? <Lock className="w-4 h-4 text-rose-500" /> : <Unlock className="w-4 h-4 text-emerald-500" />}
                        계정 상태
                    </div>
                    <Badge color={selectedEmployee.isLocked ? 'rose' : 'emerald'}>
                        {selectedEmployee.isLocked ? '잠김' : '정상'}
                    </Badge>
                </div>

                <div className="flex items-center justify-between py-3 border-b border-slate-50">
                    <div className="flex items-center gap-2 text-sm text-slate-500 font-bold">
                        활성화 여부
                    </div>
                    <Badge color={selectedEmployee.isDeleted ? 'rose' : 'emerald'} variant={selectedEmployee.isDeleted ? 'filled' : 'subtle'}>
                        {selectedEmployee.isDeleted ? '삭제됨' : '활성'}
                    </Badge>
                </div>

                <div className="pt-4 text-[11px] text-slate-400 font-medium text-center">
                    등록일시: {formatDate(selectedEmployee.createdAt, true, "dot")}
                </div>
            </div>

            <div className="pt-6 mt-6 space-y-3">
                <Button
                    variant="primary"
                    fullWidth
                    onClick={() => setIsResetModalOpen(true)}
                >
                    비밀번호 초기화
                </Button>
                <Button
                    variant="danger"
                    fullWidth
                    onClick={() => setIsDeleteModalOpen(true)}
                >
                    직원 계정 삭제
                </Button>
            </div>

            {selectedEmployee && (
                <>
                    <Modal
                        isOpen={isDeleteModalOpen}
                        onClose={() => setIsDeleteModalOpen(false)}
                        title="직원 삭제 확인"
                        maxWidth="xl"
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
                        maxWidth="xl"
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
                        maxWidth="xl"
                    >
                        <div className="space-y-6">
                            <p className="text-slate-600 leading-relaxed">
                                직원 <span className="font-bold text-slate-900">{selectedEmployee.loginId}</span>의 비밀번호가 성공적으로 변경되었습니다.
                            </p>
                            <div className="flex justify-end">
                                <Button
                                    variant="primary"
                                    onClick={() => setShowResetSuccess(false)}
                                    className="px-8"
                                >
                                    확인
                                </Button>
                            </div>
                        </div>
                    </Modal>
                </>
            )}
        </div>
    );
};

export default EmployeeDetailPanel;
