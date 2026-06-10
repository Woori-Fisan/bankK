import React from 'react';
import { cn } from '../../utils/cn';
import type { LucideIcon } from 'lucide-react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement | HTMLSelectElement> {
  label?: string;
  icon?: LucideIcon;
  error?: string;
  helperText?: string;
  as?: 'input' | 'select';
}

const Input = React.forwardRef<HTMLInputElement | HTMLSelectElement, InputProps>(
  ({ label, icon: Icon, error, helperText, className, as = 'input', children, ...props }, ref) => {
    const Component = as as any;
    const [localError, setLocalError] = React.useState<string | undefined>(undefined);

    const handleBlur = (e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) => {
      if (props.onBlur) {
        props.onBlur(e);
      }

      const val = e.target.value;
      if (!val || !val.trim()) {
        if (label) {
          const cleanLabel = label.replace(/^\d+\.\s*/, '').trim(); // "1. 고객 성명" -> "고객 성명"
          
          // 연락처는 미입력 검증에서 제외
          if (cleanLabel.includes('연락처') || props.type === 'tel') {
            setLocalError(undefined);
            return;
          }

          const isSelect = as === 'select';
          const particle = (char: string) => {
            const code = char.charCodeAt(char.length - 1) - 0xac00;
            if (code < 0 || code > 11172) return '을/를';
            return code % 28 === 0 ? '를' : '을';
          };
          const action = isSelect ? '선택해주세요.' : '입력해주세요.';
          setLocalError(`${cleanLabel}${particle(cleanLabel)} ${action}`);
        } else {
          setLocalError('필수 입력 항목입니다.');
        }
      } else {
        setLocalError(undefined);
      }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      if (props.onChange) {
        props.onChange(e);
      }
      if (e.target.value.trim()) {
        setLocalError(undefined);
      }
    };

    const displayError = error || localError;
    
    return (
      <div className="w-full space-y-2">
        {label && (
          <label className="block text-sm font-bold text-slate-700 ml-1 flex items-center gap-2">
            {Icon && <Icon className="w-4 h-4 text-emerald-500" />}
            {label}
          </label>
        )}
        
        <div className="relative">
          <Component
            ref={ref}
            className={cn(
              'w-full px-5 py-4 rounded-2xl border-2 bg-slate-50 text-slate-900 focus:bg-white outline-none transition-all appearance-none font-medium placeholder:text-slate-400',
              displayError 
                ? 'border-rose-400 focus:border-rose-500' 
                : 'border-slate-50 focus:border-emerald-500',
              as === 'select' && 'pr-12 cursor-pointer',
              className
            )}
            {...props}
            onBlur={handleBlur}
            onChange={handleChange}
          >
            {children}
          </Component>
          
          {as === 'select' && (
            <div className="absolute right-5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
              </svg>
            </div>
          )}
        </div>

        {displayError ? (
          <p className="mt-2 text-sm text-rose-500 font-bold ml-1 animate-in fade-in slide-in-from-top-1">{displayError}</p>
        ) : helperText ? (
          <p className="mt-1.5 text-[11px] text-slate-400 font-medium ml-1">{helperText}</p>
        ) : (
          <p className="mt-2 text-sm select-none pointer-events-none opacity-0">&nbsp;</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
