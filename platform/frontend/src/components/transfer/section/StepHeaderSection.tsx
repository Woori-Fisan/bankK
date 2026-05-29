import React from 'react';

interface Props {
    title: string;
    description?: string;
    center?: boolean;
}

const StepHeaderSection: React.FC<Props> = ({ title, description, center }) => (
    <div className={`mb-10 ${center ? 'text-center' : ''}`}>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{title}</h1>
        {description && <p className="text-gray-500">{description}</p>}
    </div>
);

export default StepHeaderSection;
