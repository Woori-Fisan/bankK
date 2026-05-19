import React, { useState, useRef } from 'react';
import { User, Building2, Upload, FileText, X, ChevronLeft, ChevronRight, FileType, CheckCircle2, Globe } from 'lucide-react';

interface LoanRequestFormProps {
    onNext: (data: any) => void;
    onBack: () => void;
}

const LoanRequestForm: React.FC<LoanRequestFormProps> = ({ onNext, onBack }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [formData, setFormData] = useState({
        userName: '',
        rrn: '',
        phone: '',
        bank: '',
        accountNo: '',
        accountHolder: '홍길동',
    });

    const [files, setFiles] = useState([
        { id: 1, name: '신분증_사본.pdf', progress: 100, status: '완료' },
        { id: 2, name: '소득증빙_2023.pdf', progress: 100, status: '완료' },
    ]);

    const [terms, setTerms] = useState([
        { id: 'T001', name: '신용정보조회 동의서', required: true, agreed: false },
        { id: 'T002', name: '개인(신용)정보 수집·이용·제공 동의서', required: true, agreed: false },
        { id: 'T003', name: '소득확인 동의서', required: true, agreed: false },
        { id: 'T004', name: '개인정보 제3자 제공 동의서', required: true, agreed: false },
        { id: 'T005', name: '마케팅 정보 수신 동의', required: false, agreed: false },
    ]);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTerm, setActiveTerm] = useState<any>(null);

    const handleTermToggle = (id: string) => {
        setTerms(prev => prev.map(t => t.id === id ? { ...t, agreed: !t.agreed } : t));
    };

    const handleAllAgreed = () => {
        const allAgreed = terms.every(t => t.agreed);
        setTerms(prev => prev.map(t => ({ ...t, agreed: !allAgreed })));
    };

    const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFiles = e.target.files;
        if (selectedFiles) {
            const newFiles = Array.from(selectedFiles).map((file, index) => ({
                id: Date.now() + index,
                name: file.name,
                progress: 100,
                status: '완료'
            }));
            setFiles(prev => [...prev, ...newFiles]);
        }
    };

    const handleFileDelete = (id: number) => {
        setFiles(prev => prev.filter(f => f.id !== id));
    };

    const openModal = (term: any) => {
        setActiveTerm(term);
        setIsModalOpen(true);
    };

    const handleModalAgree = () => {
        if (activeTerm) {
            handleTermToggle(activeTerm.id);
        }
        setIsModalOpen(false);
    };

    const isNextDisabled = !formData.userName || !formData.rrn || !formData.accountNo || terms.filter(t => t.required).some(t => !t.agreed);

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                {/* 본인 정보 */}
                <div className="space-y-6">
                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                            <User className="w-5 h-5 text-gray-400" />
                            <h3 className="text-sm font-bold text-gray-900">본인 정보</h3>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">성명</label>
                                <input 
                                    type="text" 
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="예) 홍길동"
                                    value={formData.userName}
                                    onChange={(e) => setFormData({...formData, userName: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">주민등록번호</label>
                                <input 
                                    type="text" 
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="900101-1******"
                                    value={formData.rrn}
                                    onChange={(e) => setFormData({...formData, rrn: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">연락처</label>
                                <input 
                                    type="text" 
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                                    placeholder="010-1234-5678"
                                    value={formData.phone}
                                    onChange={(e) => setFormData({...formData, phone: e.target.value})}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                            <Building2 className="w-5 h-5 text-gray-400" />
                            <h3 className="text-sm font-bold text-gray-900">대출금 입금 계좌</h3>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">은행 선택</label>
                                <select 
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none"
                                    value={formData.bank}
                                    onChange={(e) => setFormData({...formData, bank: e.target.value})}
                                >
                                    <option value="">은행을 선택하세요</option>
                                    <option value="우리">우리은행</option>
                                    <option value="신한">신한은행</option>
                                    <option value="국민">KB국민은행</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">계좌 번호</label>
                                <input 
                                    type="text" 
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none"
                                    placeholder="숫자만 입력"
                                    value={formData.accountNo}
                                    onChange={(e) => setFormData({...formData, accountNo: e.target.value})}
                                />
                            </div>
                            <div>
                                <label className="block text-[11px] text-gray-500 mb-1">예금주</label>
                                <input 
                                    type="text" 
                                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm outline-none"
                                    value={formData.accountHolder}
                                    onChange={(e) => setFormData({...formData, accountHolder: e.target.value})}
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* 서류 업로드 */}
                <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                        <Upload className="w-5 h-5 text-gray-400" />
                        <h3 className="text-sm font-bold text-gray-900">서류 업로드</h3>
                    </div>
                    <p className="text-[10px] text-gray-500 mb-4">필수 서류를 업로드해주세요. (PDF, 최대 10MB)</p>
                    
                    <input 
                        type="file" 
                        ref={fileInputRef} 
                        className="hidden" 
                        multiple 
                        accept=".pdf"
                        onChange={handleFileUpload}
                    />
                    <div 
                        onClick={() => fileInputRef.current?.click()}
                        className="border-2 border-dashed border-gray-200 rounded-xl p-8 text-center hover:border-blue-400 hover:bg-blue-50/30 transition-all cursor-pointer group"
                    >
                        <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3 group-hover:bg-blue-100 group-hover:text-blue-600 transition-colors">
                            <Upload className="w-6 h-6 text-gray-400 group-hover:text-blue-600" />
                        </div>
                        <p className="text-xs text-gray-600 font-medium">클릭하거나 파일을 드래그하세요</p>
                    </div>

                    <div className="mt-6 space-y-3">
                        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">업로드된 파일</p>
                        {files.length === 0 && (
                            <p className="text-xs text-gray-400 text-center py-4">업로드된 파일이 없습니다.</p>
                        )}
                        {files.map(file => (
                            <div key={file.id} className="p-3 bg-gray-50 border border-gray-100 rounded-lg flex items-center gap-3">
                                <div className="w-8 h-8 bg-white border border-gray-200 rounded flex items-center justify-center">
                                    <FileType className="w-4 h-4 text-red-500" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex justify-between mb-1">
                                        <p className="text-xs font-medium text-gray-900 truncate">{file.name}</p>
                                        <span className="text-[10px] text-gray-500">{file.status}</span>
                                    </div>
                                    <div className="h-1 bg-gray-200 rounded-full overflow-hidden">
                                        <div className="h-full bg-emerald-500 transition-all duration-500" style={{ width: `${file.progress}%` }}></div>
                                    </div>
                                </div>
                                <button 
                                    onClick={() => handleFileDelete(file.id)}
                                    className="text-gray-400 hover:text-red-500 transition-colors"
                                >
                                    <X className="w-4 h-4" />
                                </button>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 약관 동의 */}
                <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                        <FileText className="w-5 h-5 text-gray-400" />
                        <h3 className="text-sm font-bold text-gray-900">약관 동의</h3>
                    </div>
                    <p className="text-[10px] text-gray-500 mb-4">각 항목의 <strong>내용 보기</strong>를 눌러 확인 후 동의해주세요.</p>
                    
                    <button 
                        onClick={handleAllAgreed}
                        className="w-full p-3 mb-4 bg-gray-900 text-white rounded-lg text-xs font-bold flex items-center justify-center gap-2"
                    >
                        <CheckCircle2 className="w-4 h-4" />
                        전체 약관에 동의합니다
                    </button>

                    <div className="space-y-2">
                        {terms.map(term => (
                            <div key={term.id} className="p-3 bg-gray-50 rounded-lg border border-gray-100 hover:border-blue-200 transition-colors">
                                <div className="flex items-center gap-3 mb-2">
                                    <input 
                                        type="checkbox" 
                                        checked={term.agreed} 
                                        onChange={() => handleTermToggle(term.id)}
                                        className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                    />
                                    <span className="flex-1 text-xs text-gray-900 font-medium">{term.name}</span>
                                    <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${term.required ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                                        {term.required ? '필수' : '선택'}
                                    </span>
                                </div>
                                <div className="flex justify-end">
                                    <button 
                                        onClick={() => openModal(term)}
                                        className="text-[10px] text-blue-600 font-medium hover:underline"
                                    >
                                        내용 보기
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="flex justify-between pt-6 border-t border-gray-200">
                <button 
                    onClick={onBack}
                    className="flex items-center gap-2 px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-sm hover:bg-gray-50 transition-colors"
                >
                    <ChevronLeft className="w-4 h-4" />
                    이전으로
                </button>
                <button 
                    onClick={() => onNext(formData)}
                    disabled={isNextDisabled}
                    className={`flex items-center gap-2 px-6 py-3 rounded-xl font-bold text-sm transition-all shadow-md ${
                        isNextDisabled 
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed' 
                        : 'bg-slate-900 text-white hover:bg-slate-800'
                    }`}
                >
                    심사 요청하기
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>

            {/* 약관 모달 */}
            {isModalOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-3xl shadow-2xl w-full max-w-xl overflow-hidden animate-in fade-in zoom-in duration-200">
                        <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <FileText className="w-5 h-5 text-slate-400" />
                                <h3 className="text-sm font-bold">{activeTerm?.name}</h3>
                            </div>
                            <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white transition-colors">
                                <X className="w-6 h-6" />
                            </button>
                        </div>
                        <div className="h-[400px] bg-gray-50 flex flex-col items-center justify-center p-10 text-center">
                            <div className="w-16 h-16 bg-white border border-gray-200 rounded-2xl flex items-center justify-center mb-4 text-gray-300">
                                <Globe className="w-8 h-8" />
                            </div>
                            <h4 className="text-sm font-bold text-gray-900 mb-2">CDN에서 약관 내용을 불러옵니다</h4>
                            <p className="text-[11px] text-gray-500 mb-6">
                                terms_url: https://cdn.bank.com/terms/{activeTerm?.id}/v1.2
                            </p>
                            <div className="p-4 bg-blue-50 text-blue-600 rounded-xl text-[11px] font-medium max-w-sm">
                                본 영역은 실제 배포 시 iframe을 통해 은행의 표준 약관 HTML이 렌더링되는 영역입니다.
                            </div>
                        </div>
                        <div className="p-5 border-t border-gray-100 flex justify-end gap-3">
                            <button 
                                onClick={() => setIsModalOpen(false)}
                                className="px-5 py-2.5 bg-white border border-gray-200 text-gray-600 rounded-xl font-bold text-xs"
                            >
                                닫기
                            </button>
                            <button 
                                onClick={handleModalAgree}
                                className="px-6 py-2.5 bg-slate-900 text-white rounded-xl font-bold text-xs hover:bg-slate-800"
                            >
                                동의하고 닫기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default LoanRequestForm;
