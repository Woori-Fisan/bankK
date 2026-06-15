import React from 'react';
import { ChevronRight, Loader2 } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';

interface SummaryItem {
    label: string;
    value: React.ReactNode;
    placeholder?: boolean;
}

interface LoanSideSummaryProps {
    title: string;
    items: SummaryItem[];
    buttonText: string;
    onButtonClick: () => void;
    onBackClick: () => void;
    isButtonDisabled: boolean;
    isPending: boolean;
    extraContent?: React.ReactNode;
}

const LoanSideSummary: React.FC<LoanSideSummaryProps> = ({
    title,
    items,
    buttonText,
    onButtonClick,
    onBackClick,
    isButtonDisabled,
    isPending,
    extraContent,
}) => {
    return (
        <div className="sticky top-10 space-y-6">
            <Card padding="lg" className="bg-white border-slate-100 shadow-sm flex flex-col justify-between min-h-[520px]">
                <div className="space-y-8">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-bold text-slate-700 uppercase tracking-widest">{title}</span>
                    </div>
                    
                    <div className="space-y-6">
                        {items.map((item, idx) => (
                            <React.Fragment key={item.label}>
                                <div className="space-y-2">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase">{item.label}</p>
                                    <div className={item.placeholder ? "text-sm font-bold text-slate-300 italic pl-1" : "text-sm font-black text-slate-900"}>{item.value}</div>
                                </div>
                                {idx < items.length - 1 && <div className="w-full h-px bg-slate-50"></div>}
                            </React.Fragment>
                        ))}

                        {extraContent}
                    </div>
                </div>

                <div className="mt-10 space-y-3">
                    <Button
                        onClick={onButtonClick}
                        disabled={isButtonDisabled || isPending}
                        variant={isButtonDisabled || isPending ? 'secondary' : 'primary'}
                        size="xl"
                        fullWidth
                        className={`h-20 rounded-2xl text-xl font-black shadow-lg transition-all group ${
                            isButtonDisabled || isPending ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-slate-900 text-white hover:bg-slate-800'
                        }`}
                    >
                        {isPending ? (
                            <Loader2 className="w-6 h-6 animate-spin" />
                        ) : (
                            <>
                                {buttonText}
                                <ChevronRight className={`w-6 h-6 ml-2 transition-transform ${isButtonDisabled ? '' : 'group-hover:translate-x-1'}`} />
                            </>
                        )}
                    </Button>
                    <Button
                        onClick={onBackClick}
                        variant="outline"
                        fullWidth
                        className="h-12 border-none text-slate-400 font-bold hover:text-slate-600"
                    >
                        이전 단계로
                    </Button>
                </div>
            </Card>
        </div>
    );
};

export default LoanSideSummary;
