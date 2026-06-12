import React from 'react';
import { AlertCircle } from 'lucide-react';
import { cn } from '../../utils/cn';

interface InlineErrorProps {
  message?: string | null;
  className?: string;
}

export const InlineError: React.FC<InlineErrorProps> = ({ message, className }) => {
  return (
    <div 
      className={cn(
        "h-12 w-full rounded-xl flex items-center gap-2 px-4 border transition-all duration-300 ease-in-out mb-4",
        message 
          ? "opacity-100 text-rose-500 bg-rose-50/50 border-rose-100/50" 
          : "opacity-0 border-transparent bg-transparent pointer-events-none select-none",
        className
      )}
    >
      <AlertCircle className="w-4 h-4 flex-shrink-0" />
      <span className="text-xs font-bold">{message || ""}</span>
    </div>
  );
};

export default InlineError;
