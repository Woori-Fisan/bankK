import React from 'react';
import CommonConfirmModal from '../../common/CommonConfirmModal';
import { CheckCircle2 } from 'lucide-react';

interface ConfirmItem {
    label: string;
    value: React.ReactNode;
}

interface LoanConfirmModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title: string;
    description: string;
    items: ConfirmItem[];
    headerColor?: string;
    confirmButtonText?: string;
    extraChecklist?: { label: string; covered: boolean }[];
    bottomInfo?: string;
}

const LoanConfirmModal: React.FC<LoanConfirmModalProps> = ({
    isOpen,
    onClose,
    onConfirm,
    title,
    description,
    items,
    headerColor,
    confirmButtonText,
    extraChecklist,
    bottomInfo,
}) => {
    const checklistUI = extraChecklist && (
        <div className="space-y-3 mt-8">
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">필수 체크 사항</p>
            <div className="grid grid-cols-2 gap-3">
                {extraChecklist.map((doc) => (
                    <div
                        key={doc.label}
                        className="flex items-center gap-3 px-4 py-3 bg-white border border-slate-100 rounded-2xl"
                    >
                        <CheckCircle2 className={`w-4 h-4 shrink-0 ${doc.covered ? 'text-emerald-500' : 'text-slate-200'}`} />
                        <span className="text-xs font-bold text-slate-700">{doc.label}</span>
                    </div>
                ))}
            </div>
        </div>
    );

    return (
        <CommonConfirmModal
            isOpen={isOpen}
            onClose={onClose}
            onConfirm={onConfirm}
            title={title}
            description={description}
            items={items}
            headerColor={headerColor}
            confirmButtonText={confirmButtonText}
            bottomInfo={bottomInfo}
        >
            {checklistUI}
        </CommonConfirmModal>
    );
};

export default LoanConfirmModal;

