import React from 'react';

const Footer: React.FC = () => {
    return (
        <footer className="bg-white border-t border-gray-200 px-8 py-4 flex items-center justify-between">
            <div className="flex items-center gap-6">
                <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-emerald-500 rounded-full" />
                    <span className="text-xs text-gray-600">Core API Status: Optimal</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-emerald-500 rounded-full" />
                    <span className="text-xs text-gray-600">Ledger Sync: 0ms lag</span>
                </div>
            </div>
            <span className="text-xs text-gray-500">Last login: 2023-10-24 09:12:45 UTC</span>
        </footer>
    );
};

export default Footer;
