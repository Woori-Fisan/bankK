import React from 'react';
import AmountInput from '../../common/AmountInput';

interface AmountInputSectionProps {
    amount: string;
    onAmountChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onQuickAmountAdd: (value: number) => void;
    onAllIn: () => void;
    disabled?: boolean;
}

const AmountInputSection: React.FC<AmountInputSectionProps> = ({
    amount,
    onAmountChange,
    onQuickAmountAdd,
    onAllIn,
    disabled = false,
}) => {
    return (
        <AmountInput 
            label="출금 금액"
            value={amount}
            onChange={(val) => onAmountChange({ target: { value: val } } as React.ChangeEvent<HTMLInputElement>)}
            onQuickAdd={onQuickAmountAdd}
            onAllIn={onAllIn}
            disabled={disabled}
        />
    );
};

export default AmountInputSection;
