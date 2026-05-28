import React, { useState } from 'react';
import { Search, RotateCcw } from 'lucide-react';

const DateTimeInput: React.FC<{ placeholder: string }> = ({ placeholder }) => {
    const [value, setValue] = useState('');
    return (
        <div className="relative">
            <input
                type="datetime-local"
                value={value}
                onChange={e => setValue(e.target.value)}
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

const LogFilter: React.FC = () => {
    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">날짜 범위</label>
                <div className="flex gap-2">
                    <DateTimeInput placeholder="시작 날짜·시간" />
                    <span className="self-center text-gray-400">~</span>
                    <DateTimeInput placeholder="종료 날짜·시간" />
                </div>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">TRACE ID</label>
                <input 
                    type="text" 
                    placeholder="TX ID 입력"
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none w-[140px]" 
                />
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">은행명</label>
                <select className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none bg-white min-w-[120px]">
                    <option>전체 은행</option>
                    <option>국민은행</option>
                    <option>신한은행</option>
                    <option>우리은행</option>
                    <option>하나은행</option>
                    <option>농협은행</option>
                </select>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">로그 레벨</label>
                <select className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none bg-white min-w-[100px]">
                    <option>전체</option>
                    <option>INFO</option>
                    <option>WARN</option>
                    <option>ERROR</option>
                </select>
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">Error code</label>
                <input
                    type="text"
                    placeholder="에러코드(예: 404)"
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none w-[120px]"
                />
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">대행기관</label>
                <input
                    type="text"
                    placeholder="기관명 입력"
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none w-[130px]"
                />
            </div>

            <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-gray-500 ml-1">대행업자</label>
                <input
                    type="text"
                    placeholder="업자명 입력"
                    className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none w-[130px]"
                />
            </div>

<div className="flex gap-2">
                <button className="flex items-center gap-2 px-4 py-2 bg-emerald-700 text-white rounded-lg text-sm font-bold hover:bg-emerald-800 transition-colors">
                    <Search size={16} />
                    조회
                </button>
                <button className="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm font-bold hover:bg-gray-200 transition-colors" title="초기화">
                    <RotateCcw size={16} />
                </button>
            </div>
        </div>
    );
};

export default LogFilter;
