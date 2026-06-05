import React from 'react';
import { AlertTriangle, Info } from 'lucide-react';
import { Button } from './Button';

interface ConfirmItem {
    label: string;
    value: React.ReactNode;
}

interface CommonConfirmModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title: string;
    description: string;
    items: ConfirmItem[];
    headerColor?: string;
    confirmButtonText?: string;
    bottomInfo?: string;
    children?: React.ReactNode;
}

const CommonConfirmModal: React.FC<CommonConfirmModalProps> = ({
    isOpen,
    onClose,
    onConfirm,
    title,
    description,
    items,
    headerColor = 'bg-emerald-600',
    confirmButtonText = '확인 완료',
    bottomInfo,
    children,
}) => {
    if (!isOpen) return null;

    // Tailwind 정적 분석을 위해 그림자 클래스를 매핑하여 정의
    const shadowMap: Record<string, string> = {
        'bg-emerald-600': 'shadow-emerald-600/20',
        'bg-blue-600': 'shadow-blue-600/20',
        'bg-rose-600': 'shadow-rose-600/20',
        'bg-slate-900': 'shadow-slate-900/20'
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
            <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                {/* 헤더 */}
                <div className={`${headerColor} px-10 py-8 flex items-center gap-6`}>
                    <div className="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-md">
                        <AlertTriangle className="w-8 h-8 text-white" />
                    </div>
                    <div>
                        <h3 className="text-2xl font-black text-white">{title}</h3>
                        <p className="text-white/80 mt-1 font-medium">{description}</p>
                    </div>
                </div>

                <div className="p-10 space-y-8">
                    <div className="bg-slate-50 rounded-3xl p-8 space-y-5 border border-slate-100">
                        {items.map((item, idx) => (
                            <React.Fragment key={item.label}>
                                <div className="flex justify-between items-center">
                                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">{item.label}</span>
                                    <span className="text-lg font-black text-slate-900">{item.value}</span>
                                </div>
                                {idx < items.length - 1 && <div className="h-px bg-slate-200/50" />}
                            </React.Fragment>
                        ))}
                    </div>

                    {children}

                    {bottomInfo && (
                        <div className="flex items-start gap-3 px-2">
                            <Info className="w-5 h-5 text-slate-400 shrink-0 mt-0.5" />
                            <p className="text-xs text-slate-500 leading-relaxed font-medium">
                                {bottomInfo}
                            </p>
                        </div>
                    )}
                </div>

                {/* 버튼 */}
                <div className="px-10 pb-10 grid grid-cols-2 gap-4">
                    <Button
                        onClick={onClose}
                        variant="secondary"
                        size="xl"
                        className="h-16 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200"
                    >
                        수정하기
                    </Button>
                    <Button
                        onClick={onConfirm}
                        variant="primary"
                        size="xl"
                        className={`h-16 rounded-2xl text-white hover:opacity-90 shadow-xl ${shadowMap[headerColor] || ''} ${headerColor}`}
                    >
                        {confirmButtonText}
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default CommonConfirmModal;
