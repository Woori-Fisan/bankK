import React from 'react';
import { cn } from '../../utils/cn';

interface PageHeaderProps {
  title: string;
  description?: string;
  icon?: React.ElementType;
  action?: React.ReactNode;
  className?: string;
  centered?: boolean;
}

const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  description,
  icon: Icon,
  action,
  className,
  centered = false,
}) => {
  return (
    <div className={cn(
      'mb-12 flex flex-col md:flex-row md:items-end justify-between gap-6',
      centered && 'items-center text-center md:items-center',
      className
    )}>
      <div className={cn('flex flex-col', centered && 'items-center')}>
        {Icon && (
          <div className="inline-flex items-center justify-center w-20 h-20 bg-emerald-100 rounded-3xl mb-6 shadow-sm">
            <Icon className="w-10 h-10 text-emerald-600" />
          </div>
        )}
        <h1 className="text-4xl font-black text-slate-900 tracking-tight mb-3">{title}</h1>
        {description && <p className="text-lg text-slate-500 font-medium">{description}</p>}
      </div>
      {action && <div className="flex-shrink-0">{action}</div>}
    </div>
  );
};

export default PageHeader;
