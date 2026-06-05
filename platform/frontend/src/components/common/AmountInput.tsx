import React from 'react';
import { cn } from '../../utils/cn';
import { formatAmount, formatToKorean } from '../../utils/formatter';
import { Button } from './Button';
import type { LucideIcon } from 'lucide-react';
import { Coins } from 'lucide-react';

export interface AmountInputProps {
  label?: string;
  icon?: LucideIcon;
  value: string | number;
  onChange: (value: string) => void;
  onQuickAdd?: (amount: number) => void;
  onAllIn?: () => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  showKoreanUnit?: boolean;
  quickAddValues?: number[]; // [10, 50, 100] 등 (단위: 만)
}

const AmountInput: React.FC<AmountInputProps> = ({
  label = '금액 입력',
  icon: Icon = Coins,
  value,
  onChange,
  onQuickAdd,
  onAllIn,
  placeholder = '0',
  disabled = false,
  className,
  showKoreanUnit = true,
  quickAddValues = [10, 50, 100],
}) => {
  const numericValue = value.toString().replace(/[^0-9]/g, '');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.replace(/[^0-9]/g, '');
    onChange(val);
  };

  return (
    <div className={cn('space-y-4 transition-all duration-300', disabled ? 'opacity-50' : 'opacity-100', className)}>
      <div className="flex items-center justify-between px-1">
        <label className="text-sm font-bold text-slate-700 flex items-center gap-2">
          {Icon && <Icon className="w-4 h-4 text-emerald-500" />}
          {label}
        </label>
        {showKoreanUnit && numericValue !== '' && numericValue !== '0' && (
          <span className="text-xs font-bold text-emerald-600 animate-in fade-in slide-in-from-right-1">
            {formatToKorean(numericValue)}
          </span>
        )}
      </div>

      <div className="relative group">
        <input
          type="text"
          inputMode="numeric"
          value={numericValue === '0' || !numericValue ? '' : formatAmount(numericValue)}
          onChange={handleChange}
          disabled={disabled}
          className={cn(
            'w-full pl-6 pr-16 py-6 text-4xl font-black text-right bg-slate-50 border-2 border-slate-50 rounded-2xl outline-none transition-all font-mono tracking-tight text-slate-900 placeholder:text-slate-300',
            !disabled && 'focus:bg-white focus:border-emerald-500',
            disabled && 'cursor-not-allowed'
          )}
          placeholder={placeholder}
        />
        <span className="absolute right-6 top-1/2 -translate-y-1/2 text-xl font-bold text-slate-300 select-none">원</span>
      </div>

      {(onQuickAdd || onAllIn) && (
        <div className="flex flex-wrap gap-2">
          {onQuickAdd && quickAddValues.map((val) => (
            <Button
              key={val}
              type="button"
              variant="secondary"
              size="md"
              onClick={() => onQuickAdd(val * 10000)}
              disabled={disabled}
              className="flex-1 text-xs p-1 rounded-xl min-w-[70px]"
            >
              +{val}만
            </Button>
          ))}
          {onAllIn && (
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={onAllIn}
              disabled={disabled}
              className="flex-1 rounded-xl border-slate-200 min-w-[70px]"
            >
              전액
            </Button>
          )}
        </div>
      )}
    </div>
  );
};

export default AmountInput;
