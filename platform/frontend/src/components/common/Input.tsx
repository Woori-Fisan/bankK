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
              error 
                ? 'border-rose-400 focus:border-rose-500' 
                : 'border-slate-50 focus:border-emerald-500',
              as === 'select' && 'pr-12 cursor-pointer',
              className
            )}
            {...props}
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

        {error ? (
          <p className="mt-2 text-sm text-rose-500 font-bold ml-1">{error}</p>
        ) : helperText ? (
          <p className="mt-1.5 text-[11px] text-slate-400 font-medium ml-1">{helperText}</p>
        ) : null}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
