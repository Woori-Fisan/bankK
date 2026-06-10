import React from 'react';
import type { LucideIcon } from 'lucide-react';

interface LoginInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label: string;
    icon: LucideIcon;
}

const LoginInput: React.FC<LoginInputProps> = ({ label, icon: Icon, ...props }) => {
    return (
        <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 ml-1">{label}</label>
            <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Icon className="h-5 w-5 text-slate-400" />
                </div>
                <input
                    {...props}
                    className="block w-full pl-10 pr-3 py-3 border border-slate-200 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm placeholder-slate-400 transition-all outline-none bg-white"
                />
            </div>
        </div>
    );
};

export default LoginInput;
