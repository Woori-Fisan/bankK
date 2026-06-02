import React from 'react';
import Badge from '../common/Badge';

interface TransactionTypeBadgeProps {
    amount: number | string;
    type?: string; // 보조 정보로 활용 가능
}

const TransactionTypeBadge: React.FC<TransactionTypeBadgeProps> = ({ amount, type }) => {
    const amountNum = Number(amount);
    const isWithdrawal = amountNum < 0;

    return (
        <Badge color={isWithdrawal ? 'rose' : 'emerald'} variant="subtle">
            {isWithdrawal ? '출금' : '입금'}
        </Badge>
    );
};

export default TransactionTypeBadge;
