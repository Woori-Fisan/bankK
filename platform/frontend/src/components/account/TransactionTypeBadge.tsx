import React from 'react';

// 거래 유형 매핑 상수 정의
const TRANSACTION_TYPE_MAP: Record<string, { label: string; color: string; icon?: React.ReactNode }> = {
    'DEPOSIT': { 
        label: '입금', 
        color: 'text-emerald-600', 
    },
    'WITHDRAW': { 
        label: '출금', 
        color: 'text-rose-500', 
    },
    'TRANSFER': { 
        label: '이체', 
        color: 'text-blue-600', 
    },
    'LOAN': { 
        label: '대출', 
        color: 'text-indigo-600', 
    },
};

interface TransactionTypeBadgeProps {
    type: string;
}

const TransactionTypeBadge: React.FC<TransactionTypeBadgeProps> = ({ type }) => {
    const currentType = TRANSACTION_TYPE_MAP[type] || { 
        label: type, 
        color: 'text-gray-500', 
    };

    return (
        <div className={`flex items-center gap-2 font-bold ${currentType.color}`}>
            <span>{currentType.label}</span>
        </div>
    );
};

export default TransactionTypeBadge;
