import React from 'react';
import { FileCheck, FilePlus, Info, ShieldCheck, ChevronRight, Loader2 } from 'lucide-react';
import { useReviewDocuments } from '../../hooks/useLoan';
import { extractApiError } from '../../hooks/useLoan';

interface LoanGuideProps {
    onNext: () => void;
}

const LoanGuide: React.FC<LoanGuideProps> = ({ onNext }) => {
    const { data, isLoading, error } = useReviewDocuments();

    const mandatoryDocs = data?.documents.filter((d) => d.isMandatory) ?? [];
    const optionalDocs = data?.documents.filter((d) => !d.isMandatory) ?? [];

    return (
        <div className="space-y-6">
            {isLoading && (
                <div className="flex items-center justify-center py-12">
                    <Loader2 className="w-8 h-8 text-emerald-500 animate-spin" />
                    <span className="ml-3 text-sm text-gray-500">서류 목록을 불러오는 중...</span>
                </div>
            )}

            {error && (
                <div className="p-4 bg-red-50 border border-red-200 rounded-xl">
                    <p className="text-sm text-red-600">{extractApiError(error)}</p>
                </div>
            )}

            {!isLoading && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <div className="space-y-6">
                        {/* 필수 서류 */}
                        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
                            <div className="p-4 border-b border-gray-100 bg-gray-50/50 flex items-center gap-2">
                                <FileCheck className="w-5 h-5 text-gray-400" />
                                <h3 className="text-sm font-bold text-gray-900">필수 제출 서류</h3>
                            </div>
                            <div className="p-4 space-y-3">
                                {mandatoryDocs.length > 0
                                    ? mandatoryDocs.map((doc, idx) => (
                                          <div
                                              key={doc.documentType}
                                              className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
                                          >
                                              <div className="w-6 h-6 flex items-center justify-center bg-blue-50 text-blue-600 text-xs font-bold rounded-full shrink-0">
                                                  {idx + 1}
                                              </div>
                                              <div className="flex-1">
                                                  <p className="text-sm font-medium text-gray-900">
                                                      {doc.documentName}
                                                  </p>
                                              </div>
                                              <span className="px-2 py-1 bg-red-50 text-red-600 text-[10px] font-bold rounded">
                                                  필수
                                              </span>
                                          </div>
                                      ))
                                    : !error && (
                                          <>
                                              {[
                                                  { id: 1, name: '신분증 사본', desc: '주민등록증 또는 운전면허증' },
                                                  { id: 2, name: '소득 증빙 서류', desc: '근로소득원천징수영수증, 급여명세서 등' },
                                                  { id: 3, name: '재직증명서', desc: '최근 3개월 이내 발급' },
                                              ].map((doc) => (
                                                  <div
                                                      key={doc.id}
                                                      className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
                                                  >
                                                      <div className="w-6 h-6 flex items-center justify-center bg-blue-50 text-blue-600 text-xs font-bold rounded-full shrink-0">
                                                          {doc.id}
                                                      </div>
                                                      <div className="flex-1">
                                                          <p className="text-sm font-medium text-gray-900">{doc.name}</p>
                                                          <p className="text-xs text-gray-500">{doc.desc}</p>
                                                      </div>
                                                      <span className="px-2 py-1 bg-red-50 text-red-600 text-[10px] font-bold rounded">
                                                          필수
                                                      </span>
                                                  </div>
                                              ))}
                                          </>
                                      )}
                            </div>
                        </div>

                        {/* 선택 서류 */}
                        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
                            <div className="p-4 border-b border-gray-100 bg-gray-50/50 flex items-center gap-2">
                                <FilePlus className="w-5 h-5 text-gray-400" />
                                <h3 className="text-sm font-bold text-gray-900">선택 제출 서류</h3>
                            </div>
                            <div className="p-4 space-y-3">
                                {optionalDocs.length > 0
                                    ? optionalDocs.map((doc, idx) => (
                                          <div
                                              key={doc.documentType}
                                              className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
                                          >
                                              <div className="w-6 h-6 flex items-center justify-center bg-gray-100 text-gray-600 text-xs font-bold rounded-full shrink-0">
                                                  {idx + 1}
                                              </div>
                                              <div className="flex-1">
                                                  <p className="text-sm font-medium text-gray-900">
                                                      {doc.documentName}
                                                  </p>
                                              </div>
                                              <span className="px-2 py-1 bg-blue-50 text-blue-600 text-[10px] font-bold rounded">
                                                  선택
                                              </span>
                                          </div>
                                      ))
                                    : !error && (
                                          <>
                                              {[
                                                  { id: 1, name: '건강보험료 납부 확인서', desc: '소득 추가 증빙 시 활용' },
                                                  { id: 2, name: '기타 자산 증빙', desc: '한도 증액 심사 시 활용' },
                                              ].map((doc) => (
                                                  <div
                                                      key={doc.id}
                                                      className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
                                                  >
                                                      <div className="w-6 h-6 flex items-center justify-center bg-gray-100 text-gray-600 text-xs font-bold rounded-full shrink-0">
                                                          {doc.id}
                                                      </div>
                                                      <div className="flex-1">
                                                          <p className="text-sm font-medium text-gray-900">{doc.name}</p>
                                                          <p className="text-xs text-gray-500">{doc.desc}</p>
                                                      </div>
                                                      <span className="px-2 py-1 bg-blue-50 text-blue-600 text-[10px] font-bold rounded">
                                                          선택
                                                      </span>
                                                  </div>
                                              ))}
                                          </>
                                      )}
                            </div>
                        </div>
                    </div>

                    <div className="space-y-6">
                        <div className="bg-white border border-gray-200 rounded-xl p-6">
                            <div className="flex items-center gap-2 mb-4">
                                <Info className="w-5 h-5 text-gray-400" />
                                <h3 className="text-sm font-bold text-gray-900">심사 안내</h3>
                            </div>
                            <ul className="space-y-3 text-xs text-gray-600 leading-relaxed">
                                <li className="flex gap-2">
                                    <span className="text-gray-400">•</span>
                                    <span>
                                        심사는 서류 제출 후{' '}
                                        <strong className="text-gray-900">수 초 ~ 수십 초</strong> 내에
                                        완료됩니다.
                                    </span>
                                </li>
                                <li className="flex gap-2">
                                    <span className="text-gray-400">•</span>
                                    <span>신용점수 600점 미만 시 대출이 거절될 수 있습니다.</span>
                                </li>
                                <li className="flex gap-2">
                                    <span className="text-gray-400">•</span>
                                    <span>DSR 40% 초과 시 대출이 제한될 수 있습니다.</span>
                                </li>
                                <li className="flex gap-2">
                                    <span className="text-gray-400">•</span>
                                    <span>법정 최고금리 연 20% 초과 상품은 취급하지 않습니다.</span>
                                </li>
                                <li className="flex gap-2">
                                    <span className="text-gray-400">•</span>
                                    <span>본 심사는 고객의 신용점수에 영향을 줄 수 있습니다.</span>
                                </li>
                            </ul>

                            <div className="mt-6 p-4 bg-emerald-50 rounded-xl">
                                <div className="flex items-center gap-2 text-emerald-700 font-bold text-xs mb-1">
                                    <ShieldCheck className="w-4 h-4" />
                                    NICE 신용점수 조회 동의 필요
                                </div>
                                <p className="text-[10px] text-emerald-600">
                                    다음 단계에서 신용정보조회 동의서에 서명이 필요합니다.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <div className="flex justify-end pt-6 border-t border-gray-200">
                <button
                    onClick={onNext}
                    className="flex items-center gap-2 px-6 py-3 bg-slate-900 text-white rounded-xl font-bold text-sm hover:bg-slate-800 transition-colors"
                >
                    서류 제출하기
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};

export default LoanGuide;
