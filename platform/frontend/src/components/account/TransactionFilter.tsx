import React, { useState } from 'react';
import { Calendar as CalendarIcon, RotateCcw, Search } from 'lucide-react';
import Card from '../common/Card';
import { Button } from '../common/Button';

interface TransactionFilterProps {
    onSearch: (filters: FilterState) => void;
}

export interface FilterState {
    startDate: string;
    endDate: string;
}

const TransactionFilter: React.FC<TransactionFilterProps> = ({ onSearch }) => {
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
        setDateError('');
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
        onSearch(defaultFilters);
    };

    return (
        <Card padding="sm" className="mb-6 flex items-center justify-between gap-4 relative z-20">
            <div className='flex flex-col gap-1'>
                <div className='flex gap-2'>
                    <CalendarIcon className="w-10 h-10 text-slate-400 flex-shrink-0" />
                    <div className="flex items-center gap-3 border-2 border-slate-50 rounded-xl px-4 py-2 bg-slate-50 max-w-[420px] focus-within:bg-white focus-within:border-emerald-500 transition-all">
                        <div className="flex items-center gap-2 flex-1">
                            <input 
                                type="date" 
                                value={filters.startDate}
                                onChange={(e) => handleFilterChange('startDate', e.target.value)}
                                className="bg-transparent border-none focus:ring-0 text-sm text-slate-700 cursor-pointer outline-none w-full font-medium" 
                            />
                        </div>
                        <span className="text-slate-300 font-bold">~</span>
                        <div className="flex items-center gap-2 flex-1">
                            <input 
                                type="date" 
                                value={filters.endDate}
                                onChange={(e) => handleFilterChange('endDate', e.target.value)}
                                className="bg-transparent border-none focus:ring-0 text-sm text-slate-700 cursor-pointer outline-none w-full font-medium" 
                            />
                        </div>
                    </div>
                </div>
                {dateError && <p className="text-xs text-rose-500 font-bold ml-12">{dateError}</p>}
            </div>
            <div className="flex items-center gap-3">
                <Button 
                    variant="ghost"
                    size="sm"
                    onClick={handleReset}
                    className="text-slate-500 hover:text-slate-700"
                >
                    <RotateCcw className="w-4 h-4 mr-1" />
                    초기화
                </Button>
                <Button 
                    variant="emerald"
                    size="md"
                    onClick={handleSearchClick}
                    className="px-8"
                >
                    <Search className="w-4 h-4 mr-2" />
                    조회
                </Button>
            </div>
        </Card>
    );
};

export default TransactionFilter;
