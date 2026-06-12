import React, { useEffect } from 'react';
import { X, FileDown } from 'lucide-react';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
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
    const uri = log.httpUri?.toLowerCase() ?? '';
    const isTransfer = uri.includes('transfer') && !uri.includes('recipient');
    const isRequestLog = log.logType === 'CONTROLLER_REQ' || log.logType === 'BANK_REQ';
    const isBankLog = log.logType === 'BANK_REQ' || log.logType === 'BANK_RES' || log.logType === 'BANK_ERR';

    const exportToPdf = () => {
        const doc = new jsPDF();

        doc.setFontSize(16);
        doc.setFont('helvetica', 'bold');
        doc.text('BankK - Log Detail', 14, 16);

        doc.setFontSize(9);
        doc.setFont('helvetica', 'normal');
        doc.text(`Log ID: #${log.logId}  |  Level: ${log.level}  |  Exported: ${new Date().toLocaleString('en-US')}`, 14, 23);

        let y = 30;

        const section = (title: string, rows: [string, string][]) => {
            autoTable(doc, {
                startY: y,
                head: [[title, '']],
                body: rows,
                styles: { fontSize: 8.5, cellPadding: 2.5 },
                headStyles: { fillColor: [15, 118, 110], fontStyle: 'bold', fontSize: 9 },
                columnStyles: { 0: { cellWidth: 45, fontStyle: 'bold', fillColor: [245, 250, 249] } },
                alternateRowStyles: { fillColor: [255, 255, 255] },
                margin: { left: 14, right: 14 },
            });
            y = (doc as jsPDF & { lastAutoTable: { finalY: number } }).lastAutoTable.finalY + 6;
        };

        section('Basic Info', [
            ['Created At', log.createdAt ?? '-'],
            ['Log Type', log.logType ?? '-'],
        ]);

        const trackingRows: [string, string][] = [
            ['Trace ID', log.traceId ?? '-'],
            ['Staff ID', log.staffId ?? '-'],
            ['Bank Code', log.bankCode ?? '-'],
            ...(isTransfer ? [['Target Code', log.targetCode ?? '-'] as [string, string]] : []),
            ['Agency Code', log.agencyCode ?? '-'],
            ['Bank Key ID', log.bankKeyId ?? '-'],
        ];
        section('Tracking / Identity', trackingRows);

        section('HTTP', [
            ['Method', log.httpMethod ?? '-'],
            ['Status Code', String(log.httpStatus ?? '-')],
            ['Elapsed (ms)', log.elapsedMs != null ? `${log.elapsedMs} ms` : '-'],
            ['Client IP', log.clientIp ?? '-'],
            ['Request URI', log.httpUri ?? '-'],
        ]);

        if (log.errorCode || log.errorMessage) {
            section('Error', [
                ['Error Code', log.errorCode ?? '-'],
                ['Error Message', log.errorMessage ?? '-'],
            ]);
        }

        if (log.bodyData) {
            autoTable(doc, {
                startY: y,
                head: [['Body Data']],
                body: [[log.bodyData]],
                styles: { fontSize: 7, cellPadding: 2.5, font: 'courier' },
                headStyles: { fillColor: [15, 118, 110], fontStyle: 'bold', fontSize: 9, font: 'helvetica' },
                margin: { left: 14, right: 14 },
            });
        }

        doc.save(`bankk-log-${log.logId}-${new Date().toISOString().slice(0, 10)}.pdf`);
    };

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
                className="relative bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col mx-4"
                onClick={(e) => e.stopPropagation()}
            >
                {/* 헤더 */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <div className="flex items-center gap-3">
                        <span className="text-sm font-bold text-gray-900">로그 상세</span>
                        <span className="font-mono text-xs text-gray-400">#{log.logId}</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${levelStyle[log.level]}`}>
                            {log.level}
                        </span>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={exportToPdf}
                            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-emerald-700 hover:bg-emerald-800 rounded-lg transition-colors"
                        >
                            <FileDown className="w-3.5 h-3.5" />
                            PDF 저장
                        </button>
                        <button
                            onClick={onClose}
                            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                        >
                            <X size={16} />
                        </button>
                    </div>
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
                        {isTransfer && <Field label="대상 기관 코드" value={log.targetCode} />}
                        <Field label="대행기관 코드" value={log.agencyCode} />
                        <Field label="은행 키 ID" value={log.bankKeyId} mono />
                    </Section>

                    <Section title="HTTP">
                        <Field label="메서드" value={log.httpMethod} />
                        {!isRequestLog && (
                            <Field
                                label="상태 코드"
                                value={
                                    <span className={`font-bold ${httpStatusColor(log.httpStatus)}`}>
                                        {log.httpStatus}
                                    </span>
                                }
                            />
                        )}
                        {!isRequestLog && (
                            <Field label="소요 시간" value={`${log.elapsedMs} ms`} />
                        )}
                        {!isBankLog && <Field label="클라이언트 IP" value={log.clientIp} mono />}
                        <Field label="요청 URI" value={log.httpUri} full mono />
                    </Section>

                    {log.errorCode || log.errorMessage ? (
                        <Section title="에러">
                            <Field label="에러 코드" value={log.errorCode} />
                            <Field label="에러 메시지" value={log.errorMessage} />
                        </Section>
                    ) : null}

                    {/* 요청/응답 본문 */}
                    <div>
                        <p className="text-xs font-bold text-gray-500 mb-2 pb-1 border-b border-gray-100">본문 데이터</p>
                        <pre className="text-[11px] font-mono text-gray-600 bg-gray-50 rounded-lg p-3 whitespace-pre-wrap overflow-x-auto">
                            {log.bodyData
                                ? (() => {
                                    try {
                                        return JSON.stringify(JSON.parse(log.bodyData), null, 2);
                                    } catch {
                                        return log.bodyData;
                                    }
                                  })()
                                : '-'}
                        </pre>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LogDetailModal;
