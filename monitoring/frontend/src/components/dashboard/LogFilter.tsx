import React, { useEffect, useState } from 'react';
import { Search, RotateCcw } from 'lucide-react';
import type { LogListRequest } from '../../types/log';
import type { Agency, Bank } from '../../types/common';

const formatLocalInput = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const getDefaultFilters = () => {
    const now = new Date();
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    return {
        startDate: formatLocalInput(yesterday),
        endDate: formatLocalInput(now),
        bankCode: '',
        level: '',
        httpStatus: '',
        agencyCode: '',
        staffId: '',
    };
};

interface DateTimeInputProps {
    placeholder: string;
    value: string;
    onChange: (value: string) => void;
}

const DateTimeInput: React.FC<DateTimeInputProps> = ({ placeholder, value, onChange }) => {
    return (
        <div className="relative">
            <input
                type="datetime-local"
                value={value}
                onChange={e => onChange(e.target.value)}
                style={!value ? { color: 'transparent' } : {}}
                className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
            />
            {!value && (
                <span className="absolute inset-0 flex items-center px-3 pr-10 text-sm text-gray-400 pointer-events-none">
                    {placeholder}
                </span>
            )}
        </div>
    );
};

// datetime-local 값(YYYY-MM-DDTHH:MM)을 API 형식(YYYY-MM-DD HH:MM:SS)으로 변환
const toApiDate = (dt: string) => {
    const base = dt.replace('T', ' ');
    return base.length === 16 ? base + ':00' : base;
};

interface LogFilterProps {
    onSearch: (request: LogListRequest) => void;
    agencies: Agency[];
    banks: Bank[];
}

type Filters = ReturnType<typeof getDefaultFilters>;

const LogFilter: React.FC<LogFilterProps> = ({ onSearch, agencies, banks }) => {
    const [filters, setFilters] = useState<Filters>(getDefaultFilters);

    const set = (key: keyof Filters) => (value: string) =>
        setFilters(prev => ({ ...prev, [key]: value }));

    const handleReset = () => setFilters(getDefaultFilters());

    const handleSearch = () => {
        if (!filters.startDate || !filters.endDate) {
            alert('시작일과 종료일을 입력해주세요.');
            return;
        }

        const request: LogListRequest = {
            startDate: toApiDate(filters.startDate),
            endDate: toApiDate(filters.endDate),
            ...(filters.bankCode && { bankCode: filters.bankCode }),
            ...(filters.level && { level: filters.level }),
            ...(filters.httpStatus && { httpStatus: filters.httpStatus }),
            ...(filters.agencyCode && { agencyCode: filters.agencyCode }),
            ...(filters.staffId && { staffId: filters.staffId }),
        };

        onSearch(request);
    };

    useEffect(() => {
        handleSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">날짜 범위</label>
                <div className="flex gap-2">
                    <DateTimeInput
                        placeholder="시작 날짜·시간"
                        value={filters.startDate}
                        onChange={set('startDate')}
                    />
                    <span className="self-center text-gray-400">~</span>
                    <DateTimeInput
                        placeholder="종료 날짜·시간"
                        value={filters.endDate}
                        onChange={set('endDate')}
                    />
                </div>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">은행명</label>
                <select
                    value={filters.bankCode}
                    onChange={e => set('bankCode')(e.target.value)}
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none bg-white min-w-[120px]"
                >
                    <option value="">전체 은행</option>
                    {banks.map(bank => (
                        <option key={bank.bankCode} value={bank.bankCode}>
                            {bank.bankName}
                        </option>
                    ))}
                </select>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">로그 레벨</label>
                <select
                    value={filters.level}
                    onChange={e => set('level')(e.target.value)}
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none bg-white min-w-[100px]"
                >
                    <option value="">전체</option>
                    <option value="INFO">INFO</option>
                    <option value="WARN">WARN</option>
                    <option value="ERROR">ERROR</option>
                </select>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">HTTP 상태 코드</label>
                <input
                    type="text"
                    placeholder="예: 200, 404"
                    value={filters.httpStatus}
                    onChange={e => set('httpStatus')(e.target.value)}
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none w-[120px]"
                />
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">대행기관</label>
                <select
                    value={filters.agencyCode}
                    onChange={e => set('agencyCode')(e.target.value)}
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none bg-white min-w-[120px]"
                >
                    <option value="">전체 기관</option>
                    {agencies.map(agency => (
                        <option key={agency.agencyCode} value={agency.agencyCode}>
                            {agency.agencyName}
                        </option>
                    ))}
                </select>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">직원 ID</label>
                <input
                    type="text"
                    placeholder="직원 ID 입력"
                    value={filters.staffId}
                    onChange={e => set('staffId')(e.target.value)}
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none w-[130px]"
                />
            </div>

            <div className="flex gap-2">
                <button
                    onClick={handleSearch}
                    className="flex items-center gap-2 px-4 py-2 bg-emerald-700 text-white rounded-lg text-sm font-bold hover:bg-emerald-800 transition-colors"
                >
                    <Search size={16} />
                    조회
                </button>
                <button
                    onClick={handleReset}
                    className="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm font-bold hover:bg-gray-200 transition-colors"
                    title="초기화"
                >
                    <RotateCcw size={16} />
                </button>
            </div>
        </div>
    );
};

export default LogFilter;
