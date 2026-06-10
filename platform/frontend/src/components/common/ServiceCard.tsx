import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';

interface ServiceCardProps {
    icon: React.ElementType;
    bgIcon?: React.ElementType;
    title: string;
    description: string;
    to?: string;
    iconBg?: string;
    iconColor?: string;
    arrowBg?: string;
    arrowHoverBg?: string;
    arrowColor?: string;
}

const ServiceCard: React.FC<ServiceCardProps> = ({
    icon: Icon,
    title,
    description,
    to,
    iconBg = 'bg-gray-100',
    iconColor = 'text-gray-600',
    arrowBg = 'bg-gray-50',
    arrowHoverBg = 'group-hover:bg-gray-100',
    arrowColor = 'text-gray-400',
}) => {
    const CardContent = (
        <div className="bg-white border border-gray-200 rounded-2xl p-8 flex items-center justify-between hover:shadow-lg transition-all cursor-pointer group h-full min-h-[160px]">
            <div className="flex flex-col flex-1 min-w-0">
                <div className={`w-14 h-14 ${iconBg} rounded-2xl flex items-center justify-center mb-5`}>
                    <Icon className={`w-7 h-7 ${iconColor}`} />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">{title}</h3>
                <p className="text-sm text-gray-500 leading-relaxed">{description}</p>
            </div>
            <div className="ml-6 flex-shrink-0">
                <div className={`w-10 h-10 rounded-full ${arrowBg} ${arrowHoverBg} flex items-center justify-center transition-colors`}>
                    <ArrowRight className={`w-5 h-5 ${arrowColor} group-hover:translate-x-0.5 transition-transform`} />
                </div>
            </div>
        </div>
    );

    if (to) {
        return <Link to={to} className="block h-full">{CardContent}</Link>;
    }

    return CardContent;
};

export default ServiceCard;
