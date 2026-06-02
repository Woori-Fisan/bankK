import React from 'react';
import { useTransferStore } from '../../../store/useTransferStore';
import AmountInput from '../../common/AmountInput';

const AmountInputSection: React.FC = () => {
    const { amount, balance, updateData } = useTransferStore();
    const availableBalance = Number(balance) || 0;

    const handleAmountChange = (value: string) => {
        const numValue = Number(value);
        if (numValue <= availableBalance) {
            updateData({ amount: numValue });
        } else {
            updateData({ amount: availableBalance });
        }
    };

    const addAmount = (val: number) => {
        const nextAmount = amount + val;
        if (nextAmount <= availableBalance) {
            updateData({ amount: nextAmount });
        } else {
            updateData({ amount: availableBalance });
        }
    };

    return (
        <AmountInput 
            label="이체 금액 (KRW)"
            value={amount}
            onChange={handleAmountChange}
            onQuickAdd={addAmount}
            onAllIn={() => updateData({ amount: availableBalance })}
            quickAddValues={[1, 5, 10, 50]}
            showKoreanUnit={true}
        />
    );
};

export default AmountInputSection;
