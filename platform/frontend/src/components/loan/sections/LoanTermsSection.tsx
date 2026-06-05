import React from 'react';
import { FileText, Info, Loader2 } from 'lucide-react';
import Card from '../../common/Card';
import { Button } from '../../common/Button';
import type { ReviewDocument } from '../../../api/loanApi';

interface AgreedDoc extends ReviewDocument {
    agreed: boolean;
}

interface LoanTermsSectionProps {
    agreedDocs: AgreedDoc[];
    onTermToggle: (documentType: string) => void;
    onOpenModal: (doc: AgreedDoc) => void;
    viewedDocs: Set<string>;
    isLoading: boolean;
    title?: string;
    stepNumber?: string;
}

const LoanTermsSection: React.FC<LoanTermsSectionProps> = ({
    agreedDocs,
    onTermToggle,
    onOpenModal,
    viewedDocs,
    isLoading,
    title = "약관 동의",
    stepNumber = "3"
}) => {
    return (
        <Card padding="lg" className="border-slate-100 shadow-sm">
            <div className="flex items-center gap-2 px-1 mb-8 pb-5 border-b border-gray-100">
                <FileText className="w-4 h-4 text-emerald-500" />
                <h3 className="text-sm font-bold text-slate-700">{stepNumber}. {title}</h3>
            </div>

            <div className="bg-slate-50/50 rounded-2xl p-6 border border-slate-100 flex items-start gap-4 mb-8">
                <Info className="w-5 h-5 text-slate-400 mt-0.5 flex-shrink-0" />
                <p className="text-xs text-slate-500 leading-relaxed font-medium">
                    각 항목의 <strong className="text-slate-900">내용 보기</strong>를 눌러 약관을 확인한 후 동의해주세요.
                    필수 항목에 모두 동의해야 다음 단계로 진행할 수 있습니다.
                </p>
            </div>

            {isLoading ? (
                <div className="flex items-center justify-center py-12">
                    <Loader2 className="w-8 h-8 text-emerald-500 animate-spin" />
                </div>
            ) : (
                <div className="space-y-4">
                    <div className="mt-6 space-y-3">
                        {agreedDocs.map((doc) => (
                            <div
                                key={doc.documentType}
                                className="flex items-center gap-4 px-6 py-5 bg-white rounded-2xl border border-slate-100 hover:border-emerald-200 transition-all group"
                            >
                                <input
                                    type="checkbox"
                                    id={`doc-${doc.documentType}`}
                                    checked={doc.agreed}
                                    onChange={() => onTermToggle(doc.documentType)}
                                    disabled={!viewedDocs.has(doc.documentType)}
                                    className="w-5 h-5 rounded-lg border-slate-300 text-emerald-600 focus:ring-emerald-500 disabled:opacity-40 transition-all"
                                />
                                <label
                                    htmlFor={`doc-${doc.documentType}`}
                                    className={`flex-1 text-sm font-bold text-slate-700 ${
                                        viewedDocs.has(doc.documentType)
                                            ? 'cursor-pointer'
                                            : 'cursor-not-allowed opacity-60'
                                    }`}
                                >
                                    {doc.documentName}
                                </label>
                                <span
                                    className={`px-3 py-1 rounded-full text-[10px] font-black shrink-0 ${
                                        doc.isMandatory
                                            ? 'bg-rose-50 text-rose-600'
                                            : 'bg-blue-50 text-blue-600'
                                    }`}
                                >
                                    {doc.isMandatory ? '필수' : '선택'}
                                </span>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => onOpenModal(doc)}
                                    className="shrink-0 rounded-xl"
                                >
                                    내용 보기
                                </Button>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </Card>
    );
};

export default LoanTermsSection;
