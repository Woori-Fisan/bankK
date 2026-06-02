import React, { useState } from 'react';
import { Calendar as CalendarIcon, RotateCcw } from 'lucide-react';

interface TransactionFilterProps {
    onSearch: (filters: FilterState) => void;
}

export interface FilterState {
    startDate: string;
    endDate: string;
}

const TransactionFilter: React.FC<TransactionFilterProps> = ({ onSearch }) => {
    // 기본값: 한 달 전 ~ 오늘
    const getDefaultFilters = () => {
        const today = new Date().toISOString().split('T')[0];
        const lastMonth = new Date();
        lastMonth.setMonth(lastMonth.getMonth() - 1);
        const startDate = lastMonth.toISOString().split('T')[0];
        return { startDate, endDate: today };
    };

    const [filters, setFilters] = useState<FilterState>(getDefaultFilters());
    const [dateError, setDateError] = useState('');

    const handleFilterChange = (key: keyof FilterState, value: string) => {
        setFilters(prev => ({ ...prev, [key]: value }));
        setDateError(''); // 값 변경 시 에러 초기화
    };

    const handleSearchClick = () => {
        if (new Date(filters.startDate) > new Date(filters.endDate)) {
            setDateError('시작일은 종료일보다 이전이어야 합니다.');
            return;
        }
        setDateError('');
        onSearch(filters);
    };

    const handleReset = () => {
        const defaultFilters = getDefaultFilters();
        setFilters(defaultFilters);
        setDateError('');
        onSearch(defaultFilters); // 초기화된 날짜로 즉시 새로운 검색 요청 수행
    };

    return (
        <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm mb-6 flex items-center justify-between gap-4 relative z-20">
            {/* Date Range */}
            <div className='flex flex-col gap-1'>
                <div className='flex gap-2'>
                    <CalendarIcon className="w-10 h-10 text-gray-400 flex-shrink-0" />
                    <div className="flex items-center gap-3 border border-gray-200 rounded-lg px-4 py-2 bg-gray-50 max-w-[420px]">
                        {/* 시작일 */}
                        <div className="flex items-center gap-2 flex-1">
                            <input 
                                type="date" 
                                value={filters.startDate}
                                onChange={(e) => handleFilterChange('startDate', e.target.value)}
                                className="bg-transparent border-none focus:ring-0 text-sm text-gray-700 cursor-pointer outline-none w-full" 
                            />
                        </div>
                        
                        <span className="text-gray-300 font-bold">~</span>
                        
                        {/* 종료일 */}
                        <div className="flex items-center gap-2 flex-1">
                            
                            <input 
                                type="date" 
                                value={filters.endDate}
                                onChange={(e) => handleFilterChange('endDate', e.target.value)}
                                className="bg-transparent border-none focus:ring-0 text-sm text-gray-700 cursor-pointer outline-none w-full" 
                            />
                        </div>
                    </div>
                </div>
                {dateError && <p className="text-xs text-rose-500 font-bold ml-12">{dateError}</p>}
            </div>
            {/* Buttons */}
            <div className="flex items-center gap-2">
                <button 
                    onClick={handleReset}
                    className="flex items-center gap-1 text-sm font-medium text-gray-500 hover:text-gray-700 px-3 py-2 transition-colors"
                >
                    <RotateCcw className="w-4 h-4" />
                    초기화
                </button>
                <button 
                    onClick={handleSearchClick}
                    className="bg-emerald-800 text-white px-6 py-2 rounded-lg text-sm font-medium hover:bg-emerald-900 active:scale-95 transition-all shadow-sm"
                >
                    조회
                </button>
            </div>
        </div>
    );
};

export default TransactionFilter;
