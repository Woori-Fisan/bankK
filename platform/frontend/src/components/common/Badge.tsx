import React from 'react';
import { cn } from '../../utils/cn';

interface BadgeProps {
  children: React.ReactNode;
  color?: 'emerald' | 'rose' | 'blue' | 'amber' | 'slate';
  variant?: 'filled' | 'subtle';
  className?: string;
}

const Badge: React.FC<BadgeProps> = ({
  children,
  color = 'slate',
  variant = 'subtle',
  className,
}) => {
  const colors = {
    emerald: {
      filled: 'bg-emerald-600 text-white',
      subtle: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    },
    rose: {
      filled: 'bg-rose-600 text-white',
      subtle: 'bg-rose-50 text-rose-700 border-rose-100',
    },
    blue: {
      filled: 'bg-blue-600 text-white',
      subtle: 'bg-blue-50 text-blue-700 border-blue-100',
    },
    amber: {
      filled: 'bg-amber-500 text-white',
      subtle: 'bg-amber-50 text-amber-700 border-amber-100',
    },
    slate: {
      filled: 'bg-slate-600 text-white',
      subtle: 'bg-slate-50 text-slate-600 border-slate-200',
    },
  };

  return (
    <span
      className={cn(
        'inline-flex items-center px-3 py-1 rounded-full text-xs font-black border transition-all',
        colors[color][variant],
        className
      )}
    >
      {children}
    </span>
  );
};

export default Badge;
