import React, { useEffect } from 'react';
import { X } from 'lucide-react';
import type { SystemLog } from '../../types/log';

interface Props {
    log: SystemLog;
    onClose: () => void;
}

const levelStyle: Record<SystemLog['level'], string> = {
    INFO:  'bg-blue-100 text-blue-600',
    WARN:  'bg-amber-100 text-amber-600',
    ERROR: 'bg-red-100 text-red-600',
};

const httpStatusColor = (status: number) => {
    if (status >= 500) return 'text-red-600';
    if (status >= 400) return 'text-amber-600';
    return 'text-emerald-600';
};

interface FieldProps {
    label: string;
    value: React.ReactNode;
    full?: boolean;
    mono?: boolean;
}

const Field: React.FC<FieldProps> = ({ label, value, full, mono }) => (
    <div className={full ? 'col-span-2' : ''}>
        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">{label}</p>
        <p className={`text-sm text-gray-800 break-all ${mono ? 'font-mono text-xs' : ''}`}>
            {value ?? <span className="text-gray-300">-</span>}
        </p>
    </div>
);

const Section: React.FC<{ title: string; children: React.ReactNode }> = ({ title, children }) => (
    <div>
        <p className="text-xs font-bold text-gray-500 mb-3 pb-1 border-b border-gray-100">{title}</p>
        <div className="grid grid-cols-2 gap-x-6 gap-y-4">
            {children}
        </div>
    </div>
);

const LogDetailModal: React.FC<Props> = ({ log, onClose }) => {
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [onClose]);

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
            onClick={onClose}
        >
            <div
                className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col mx-4"
                onClick={(e) => e.stopPropagation()}
            >
                {/* 헤더 */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <div className="flex items-center gap-3">
                        <span className="text-sm font-bold text-gray-900">로그 상세</span>
                        <span className="font-mono text-xs text-gray-400">#{log.id}</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${levelStyle[log.level]}`}>
                            {log.level}
                        </span>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                    >
                        <X size={16} />
                    </button>
                </div>

                {/* 본문 */}
                <div className="overflow-y-auto px-6 py-5 space-y-6">
                    <Section title="기본 정보">
                        <Field label="생성 일시" value={log.createdAt} />
                        <Field label="로그 유형" value={log.logType} />
                    </Section>

                    <Section title="추적 / 식별">
                        <Field label="Trace ID" value={log.traceId} mono />
                        <Field label="담당 직원 ID" value={log.staffId} mono />
                        <Field label="은행 코드" value={log.bankCode} />
                        <Field label="대상 기관 코드" value={log.targetCode} />
                        <Field label="대행기관 코드" value={log.agencyCode} />
                        <Field label="은행 키 ID" value={log.bankKeyId} mono />
                    </Section>

                    <Section title="HTTP">
                        <Field label="메서드" value={log.httpMethod} />
                        <Field
                            label="상태 코드"
                            value={
                                <span className={`font-bold ${httpStatusColor(log.httpStatus)}`}>
                                    {log.httpStatus}
                                </span>
                            }
                        />
                        <Field label="소요 시간" value={`${log.elapsedMs} ms`} />
                        <Field label="클라이언트 IP" value={log.clientIp} mono />
                        <Field label="요청 URI" value={log.httpUri} full mono />
                    </Section>

                    {log.errorCode || log.errorMessage ? (
                        <Section title="에러">
                            <Field label="에러 코드" value={log.errorCode} />
                            <Field label="에러 메시지" value={log.errorMessage} />
                        </Section>
                    ) : null}

                    {/* JWS 서명 */}
                    <div>
                        <p className="text-xs font-bold text-gray-500 mb-2 pb-1 border-b border-gray-100">JWS 서명</p>
                        <pre className="text-[11px] font-mono text-gray-600 bg-gray-50 rounded-lg p-3 break-all whitespace-pre-wrap overflow-x-auto">
                            {log.jwsSignature || '-'}
                        </pre>
                    </div>

                    {/* 요청/응답 본문 */}
                    <div>
                        <p className="text-xs font-bold text-gray-500 mb-2 pb-1 border-b border-gray-100">본문 데이터</p>
                        <pre className="text-[11px] font-mono text-gray-600 bg-gray-50 rounded-lg p-3 break-all whitespace-pre-wrap overflow-x-auto">
                            {log.bodyData || '-'}
                        </pre>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LogDetailModal;
