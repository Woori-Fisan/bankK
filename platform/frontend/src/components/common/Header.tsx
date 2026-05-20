import React from 'react';

interface HeaderProps {
    currentTime: string;
    sessionTime: string;
}

const Header: React.FC<HeaderProps> = ({ currentTime, sessionTime }) => {
    return (
        <header className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-end">
            <div className="flex items-center gap-6">
                <div className="flex items-center gap-3">
                    <div className="w-9 h-9 bg-emerald-700 rounded-full flex items-center justify-center">
                        <span className="text-white text-sm font-medium">U</span>
                    </div>
                    <span className="text-sm font-medium text-gray-900">사용자 명</span>
                </div>
                <div className="text-right">
                    <div className="text-xs text-gray-500">
                        접속 시간 <span className="text-gray-700">{currentTime}</span>
                    </div>
                    <div className="text-xs text-gray-500">
                        인증 만료 <span className="text-gray-700">{sessionTime}</span>
                    </div>
                </div>
            </div>
        </header>
    );
};

export default Header;
