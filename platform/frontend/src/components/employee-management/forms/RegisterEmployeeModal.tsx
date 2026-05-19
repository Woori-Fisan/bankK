import React from 'react';
import ReactDOM from 'react-dom';
import EmployeeRegistrationForm from './EmployeeRegistrationForm';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

const RegisterEmployeeModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
    if (!isOpen) return null;

    return ReactDOM.createPortal(
        <div className="fixed inset-0 flex justify-center items-center z-50">
            <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-bold text-gray-900">새 직원 등록</h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
                <EmployeeRegistrationForm onSuccess={() => { onSuccess(); onClose(); }} onCancel={onClose} />
            </div>
        </div>,
        document.body
    );
};

export default RegisterEmployeeModal;
