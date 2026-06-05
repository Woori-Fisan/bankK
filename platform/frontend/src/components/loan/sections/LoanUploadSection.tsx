import React from 'react';
import { Upload, CheckCircle2, FileType, X, AlertCircle } from 'lucide-react';
import Card from '../../common/Card';

export const REQUIRED_DOCS = [
    { label: '신분증 사본', hint: '신분증.pdf / 면허증.pdf', keywords: ['신분증', '면허증'] },
    { label: '재직증명서', hint: '재직증명서.pdf', keywords: ['재직증명서'] },
    { label: '근로소득 원천징수영수증', hint: '원천징수.pdf', keywords: ['원천징수'] },
    { label: '건강보험료 납부확인서', hint: '건강보험.pdf', keywords: ['건강보험'] },
] as const;

interface LoanUploadSectionProps {
    files: { id: number; name: string; file: File }[];
    onFileSelect: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onFileDelete: (id: number) => void;
    fileInputRef: React.RefObject<HTMLInputElement>;
    fileUploadError?: string;
}

const LoanUploadSection: React.FC<LoanUploadSectionProps> = ({
    files,
    onFileSelect,
    onFileDelete,
    fileInputRef,
    fileUploadError,
}) => {
    const fileNames = files.map((f) => f.name.toLowerCase());

    const coveredDocs = REQUIRED_DOCS.map((doc) => ({
        ...doc,
        covered: fileNames.some((name) => doc.keywords.some((kw) => name.includes(kw))),
    }));

    return (
        <Card padding="lg" className="border-slate-100 shadow-sm">
            <div className="flex items-center gap-2 px-1 mb-8 pb-5 border-b border-gray-100">
                <Upload className="w-4 h-4 text-emerald-500" />
                <h3 className="text-sm font-bold text-slate-700">2. 서류 업로드</h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                {coveredDocs.map((doc) => (
                    <div
                        key={doc.label}
                        className={`flex items-center gap-4 px-5 py-4 rounded-2xl border transition-all ${
                            doc.covered
                                ? 'bg-emerald-50 border-emerald-100 text-emerald-700'
                                : 'bg-slate-50/50 border-slate-100 text-slate-400'
                        }`}
                    >
                        <CheckCircle2
                            className={`w-5 h-5 flex-shrink-0 ${
                                doc.covered ? 'text-emerald-500' : 'text-slate-200'
                            }`}
                        />
                        <div className="min-w-0">
                            <p className="font-bold text-sm">{doc.label}</p>
                            {!doc.covered && (
                                <p className="text-[10px] opacity-60 mt-0.5">{doc.hint}</p>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                multiple
                accept=".pdf"
                onChange={onFileSelect}
            />

            <div
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-200 rounded-3xl p-12 text-center hover:border-emerald-400 hover:bg-emerald-50/30 transition-all cursor-pointer group"
            >
                <div className="w-16 h-16 bg-slate-50 rounded-2xl flex items-center justify-center mx-auto mb-4 group-hover:bg-emerald-100 transition-colors">
                    <Upload className="w-8 h-8 text-slate-300 group-hover:text-emerald-500 transition-colors" />
                </div>
                <p className="text-base font-black text-slate-700 mb-1">클릭하거나 파일을 드래그하세요</p>
                <p className="text-xs text-slate-400 font-medium">PDF 파일 전용 (최대 10MB)</p>
            </div>

            {fileUploadError && (
                <div className="mt-4 flex items-start gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100 animate-in fade-in slide-in-from-top-2">
                    <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                    <span className="text-xs font-bold whitespace-pre-line">{fileUploadError}</span>
                </div>
            )}

            {files.length > 0 && (
                <div className="mt-8 space-y-3">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-4">
                        업로드된 파일 ({files.length}개)
                    </p>
                    {files.map((f) => (
                        <div
                            key={f.id}
                            className="px-5 py-4 bg-white border border-slate-100 rounded-2xl flex items-center gap-4 animate-in fade-in slide-in-from-left-2"
                        >
                            <div className="w-10 h-10 bg-rose-50 rounded-xl flex items-center justify-center shrink-0">
                                <FileType className="w-5 h-5 text-rose-500" />
                            </div>
                            <p className="flex-1 text-sm font-bold text-slate-700 truncate">{f.name}</p>
                            <button
                                type="button"
                                onClick={() => onFileDelete(f.id)}
                                className="p-2 text-slate-300 hover:text-rose-500 transition-colors shrink-0"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </Card>
    );
};

export default LoanUploadSection;
