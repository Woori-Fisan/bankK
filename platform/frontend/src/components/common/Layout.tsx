import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import Footer from './Footer';
import FloatingButton from './FloatingButton';

const Layout: React.FC = () => {
    const [currentTime] = useState<string>('2026.05.15');
    const [sessionTime] = useState<string>('00:30:10');

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

            <FloatingButton />
        </div>
    );
};

export default Layout;
