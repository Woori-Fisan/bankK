import React from 'react';
import { Link } from 'react-router-dom';

interface ServiceCardProps {
    icon: React.ElementType;
    bgIcon: React.ElementType;
    title: string;
    description: string;
    to?: string;
}

const ServiceCard: React.FC<ServiceCardProps> = ({ icon: Icon, bgIcon: BgIcon, title, description, to }) => {
    const CardContent = (
        <div className="bg-white border border-gray-200 rounded-lg p-6 relative overflow-hidden hover:shadow-md transition-shadow cursor-pointer group h-full">
            <div className="flex items-start justify-between mb-4">
                <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center">
                    <Icon className="w-6 h-6 text-gray-700" />
                </div>
                <BgIcon className="w-12 h-12 text-gray-200 absolute top-6 right-6" strokeWidth={1.5} />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
            <p className="text-sm text-gray-600 leading-relaxed">{description}</p>
        </div>
    );

    if (to) {
        return <Link to={to}>{CardContent}</Link>;
    }

    return CardContent;
};

export default ServiceCard;
