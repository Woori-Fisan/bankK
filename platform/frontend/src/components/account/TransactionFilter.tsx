import React, { useState } from 'react';
import { Calendar as CalendarIcon, Search, RotateCcw, ChevronDown } from 'lucide-react';

interface TransactionFilterProps {
    onSearch: (filters: FilterState) => void;
    onReset: () => void;
}

export interface FilterState {
    startDate: string;
    endDate: string;
    type: string;
    keyword: string;
}

const TransactionFilter: React.FC<TransactionFilterProps> = ({ onSearch, onReset }) => {
    const [filters, setFilters] = useState<FilterState>({
        startDate: '2023-10-01',
        endDate: '2023-10-24',
        type: '전체 거래',
        keyword: ''
    });

    const [isTypeOpen, setIsTypeOpen] = useState(false);
    const types = ['전체 거래', '입금', '출금'];

    const handleFilterChange = (key: keyof FilterState, value: string) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    const handleReset = () => {
        const initial = {
            startDate: '2023-10-01',
            endDate: '2023-10-24',
            type: '전체 거래',
            keyword: ''
        };
        setFilters(initial);
        onReset();
    };

    return (
        <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm mb-6 flex items-center gap-4 relative z-20">
            {/* Date Range */}
            <div className="flex items-center gap-2 border border-gray-200 rounded-lg px-3 py-2 bg-gray-50 flex-1">
                <CalendarIcon className="w-4 h-4 text-gray-400" />
                <input 
                    type="date" 
                    value={filters.startDate}
                    onChange={(e) => handleFilterChange('startDate', e.target.value)}
                    className="bg-transparent border-none focus:ring-0 text-sm w-32 text-gray-700 cursor-pointer" 
                />
                <span className="text-gray-400">~</span>
                <input 
                    type="date" 
                    value={filters.endDate}
                    onChange={(e) => handleFilterChange('endDate', e.target.value)}
                    className="bg-transparent border-none focus:ring-0 text-sm w-32 text-gray-700 cursor-pointer" 
                />
                <CalendarIcon className="w-4 h-4 text-gray-400" />
            </div>

            {/* Type Select */}
            <div className="relative">
                <button 
                    onClick={() => setIsTypeOpen(!isTypeOpen)}
                    className="flex items-center justify-between gap-2 border border-gray-200 rounded-lg px-4 py-2 bg-white text-sm text-gray-700 min-w-[120px] hover:bg-gray-50 transition-colors"
                >
                    {filters.type}
                    <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${isTypeOpen ? 'rotate-180' : ''}`} />
                </button>
                
                {isTypeOpen && (
                    <div className="absolute top-full left-0 mt-1 w-full bg-white border border-gray-100 rounded-lg shadow-xl z-50 py-1 overflow-hidden">
                        {types.map((t) => (
                            <button
                                key={t}
                                onClick={() => {
                                    handleFilterChange('type', t);
                                    setIsTypeOpen(false);
                                }}
                                className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-emerald-50 hover:text-emerald-700 transition-colors"
                            >
                                {t}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {/* Search Input */}
            <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input 
                    type="text" 
                    placeholder="적요, 거래처명 검색" 
                    value={filters.keyword}
                    onChange={(e) => handleFilterChange('keyword', e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && onSearch(filters)}
                    className="w-full border border-gray-200 rounded-lg pl-10 pr-4 py-2 text-sm focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all" 
                />
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
                    onClick={() => onSearch(filters)}
                    className="bg-emerald-800 text-white px-6 py-2 rounded-lg text-sm font-medium hover:bg-emerald-900 active:scale-95 transition-all shadow-sm"
                >
                    조회
                </button>
            </div>
        </div>
    );
};

export default TransactionFilter;
