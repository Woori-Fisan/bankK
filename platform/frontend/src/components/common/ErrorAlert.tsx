import React, { useEffect, useRef } from 'react';
import { AlertCircle } from 'lucide-react';
import { cn } from '../../utils/cn';

interface ErrorAlertProps {
  message?: string | null;
  className?: string;
  scrollContainerRef?: React.RefObject<HTMLDivElement>;
}

export const ErrorAlert: React.FC<ErrorAlertProps> = ({ message, className, scrollContainerRef }) => {
  const alertRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (message) {
      if (scrollContainerRef?.current) {
        scrollContainerRef.current.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
      } else {
        window.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
        
        alertRef.current?.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest'
        });
      }
    }
  }, [message, scrollContainerRef]);

  return (
    <div 
      ref={alertRef}
      className={cn(
        "h-14 w-full max-w-4xl mx-auto rounded-2xl flex items-center gap-2 px-4 border transition-all duration-300 ease-in-out mb-6",
        message
          ? "opacity-100 text-rose-500 bg-rose-50 border-rose-100"
          : "opacity-0 border-transparent bg-transparent pointer-events-none select-none",
        className
      )}
      role="alert"
    >
      <AlertCircle className="w-5 h-5 flex-shrink-0" />
      <span className="text-sm font-bold">{message || ""}</span>
    </div>
  );
};

export default ErrorAlert;
