import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import Footer from './Footer';
import FloatingButton from './FloatingButton';
import ChatWindow from '../rag/ChatWindow';

const Layout: React.FC = () => {
    const [currentTime] = useState<string>('2026.05.15');
    const [sessionTime] = useState<string>('00:30:10');
    const [isChatOpen, setIsChatOpen] = useState<boolean>(false);
    const [isClosing, setIsClosing] = useState<boolean>(false);

    const handleOpenChat = () => {
        setIsChatOpen(true);
        setIsClosing(false);
    };

    const handleCloseChat = () => {
        setIsClosing(true);
        // 애니메이션 시간(0.25s) 후에 컴포넌트 제거
        setTimeout(() => {
            setIsChatOpen(false);
            setIsClosing(false);
        }, 250);
    };

    return (
        <div className="flex h-screen bg-gray-50 font-sans">
            <Sidebar />

            <main className="flex-1 flex flex-col min-w-0">
                <Header currentTime={currentTime} sessionTime={sessionTime} />
                
                <div className="flex-1 flex flex-col overflow-y-auto bg-gray-50">
                    <Outlet />
                </div>

                <Footer />
            </main>

            {isChatOpen ? (
                <ChatWindow onClose={handleCloseChat} isClosing={isClosing} />
            ) : (
                <FloatingButton onClick={handleOpenChat} />
            )}
        </div>
    );
};

export default Layout;
