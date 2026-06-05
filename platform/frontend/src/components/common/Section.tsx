import React from 'react';
import { cn } from '../../utils/cn';

interface SectionProps {
  title?: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
  columns?: 1 | 2 | 3 | 4;
  gap?: number;
}

const Section: React.FC<SectionProps> = ({
  title,
  description,
  children,
  className,
  columns = 1,
  gap = 6,
}) => {
  const columnClasses = {
    1: 'grid-cols-1',
    2: 'grid-cols-1 md:grid-cols-2',
    3: 'grid-cols-1 md:grid-cols-3',
    4: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
  };

  const gapClasses: Record<number, string> = {
    2: 'gap-2',
    3: 'gap-3',
    4: 'gap-4',
    6: 'gap-6',
    8: 'gap-8',
    10: 'gap-10',
  };

  return (
    <section className={cn('w-full mb-10', className)}>
      {(title || description) && (
        <div className="mb-6">
          {title && <h2 className="text-2xl font-bold text-slate-900">{title}</h2>}
          {description && <p className="text-slate-500 mt-1">{description}</p>}
        </div>
      )}
      <div className={cn('grid', columnClasses[columns], gapClasses[gap] || 'gap-6')}>
        {children}
      </div>
    </section>
  );
};

export default Section;
