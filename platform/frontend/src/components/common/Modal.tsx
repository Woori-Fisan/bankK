import React, { useEffect } from 'react';
import ReactDOM from 'react-dom';
import { X } from 'lucide-react';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    children: React.ReactNode;
    maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl';
}

const Modal: React.FC<ModalProps> = ({ 
    isOpen, 
    onClose, 
    title, 
    children, 
    maxWidth = 'md' 
}) => {
    // ESC 키를 눌렀을 때 모달 닫기
    useEffect(() => {
        const handleEsc = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        if (isOpen) {
            window.addEventListener('keydown', handleEsc);
            // 모달 오픈 시 본문 스크롤 방지
            document.body.style.overflow = 'hidden';
        }
        return () => {
            window.removeEventListener('keydown', handleEsc);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    const maxWidthClasses = {
        sm: 'max-w-sm',
        md: 'max-w-md',
        lg: 'max-w-lg',
        xl: 'max-w-xl',
        '2xl': 'max-w-2xl',
        '3xl': 'max-w-3xl',
        '4xl': 'max-w-4xl',
    };

    return ReactDOM.createPortal(
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop */}
            <div 
                className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" 
                onClick={onClose}
            />
            
            {/* Modal Content */}
            <div className={`relative bg-white rounded-2xl shadow-2xl w-full ${maxWidthClasses[maxWidth]} transform transition-all overflow-hidden`}>
                {/* Header */}
                {title && (
                    <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-white">
                        <h3 className="text-lg font-bold text-slate-900">{title}</h3>
                        <button 
                            onClick={onClose}
                            className="p-2 rounded-full text-slate-400 hover:bg-slate-50 hover:text-slate-600 transition-colors"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>
                )}
                
                {/* Body */}
                <div className="p-6">
                    {children}
                </div>
            </div>
        </div>,
        document.body
    );
};

export default Modal;
