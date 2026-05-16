import React from 'react';
import { Building2 } from 'lucide-react';

const LoginHeader: React.FC = () => {
    return (
        <div className="flex flex-col items-center mb-10">
            <div className="w-16 h-16 bg-slate-900 rounded-full flex items-center justify-center mb-6 shadow-lg">
                <Building2 className="w-8 h-8 text-white" />
            </div>
            <div className="text-center">
                <h1 className="text-2xl font-bold text-slate-900">BaaS Institutional</h1>
                <p className="text-slate-500 text-sm mt-1">Enterprise Portal</p>
            </div>
        </div>
    );
};

export default LoginHeader;
