import React from 'react';
import { LogOut } from 'lucide-react';
import { useAuthCrypto } from '../../hooks/useAuthCrypto';

interface HeaderProps {
    currentTime: string;
    sessionTime: string;
}

const Header: React.FC<HeaderProps> = ({ currentTime, sessionTime }) => {
    const { performLogout, isLoading } = useAuthCrypto();

    const handleLogout = () => {
        if (window.confirm("로그아웃 하시겠습니까?")) {
            performLogout();
        }
    };

    return (
        <header className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-end">
            <div className="flex items-center gap-6">
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 bg-emerald-700 rounded-full flex items-center justify-center">
                            <span className="text-white text-sm font-medium">U</span>
                        </div>
                        <span className="text-sm font-medium text-gray-900">사용자 명</span>
                    </div>
                    
                    {/* 로그아웃 버튼 추가 */}
                    <button 
                        onClick={handleLogout}
                        disabled={isLoading}
                        className="flex items-center justify-center p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-red-200"
                        title="로그아웃"
                    >
                        <LogOut size={18} />
                    </button>
                </div>
                
                <div className="text-right border-l border-gray-200 pl-6">
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
