import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import {
    ClipboardList,
    ArrowLeftRight,
    Banknote,
    Search,
    UserCog,
    Landmark,
    LayoutDashboard,
} from 'lucide-react';

interface SidebarItemProps {
    icon: React.ElementType;
    label: string;
    to: string;
}

const SidebarItem: React.FC<SidebarItemProps> = ({ icon: Icon, label, to }) => (
    <NavLink
        to={to}
        className={({ isActive }) =>
            `flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors ${
                isActive
                    ? 'bg-slate-700/50 border-l-2 border-emerald-500 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
            }`
        }
    >
        <Icon className="w-5 h-5" />
        <span className="text-sm font-medium">{label}</span>
    </NavLink>
);

const Sidebar: React.FC = () => {
    return (
        <aside className="w-64 bg-slate-900 flex flex-col flex-shrink-0">
            <div className="p-6 pb-8">
                <Link to="/" className="flex items-center gap-3 cursor-pointer group">
                    <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center transition-transform group-hover:scale-105">
                        <Landmark className="w-6 h-6 text-slate-900" />
                    </div>
                    <div>
                        <h1 className="text-white text-xl font-bold leading-tight group-hover:text-emerald-400 transition-colors">Bank Bridge</h1>
                        <p className="text-slate-400 text-xs">Institutional Banking</p>
                    </div>
                </Link>
            </div>

            <nav className="flex-1">
                <SidebarItem icon={LayoutDashboard} label="대시보드" to="/" />
                <SidebarItem icon={ClipboardList} label="대출 관리" to="/loan" />
                <SidebarItem icon={ArrowLeftRight} label="계좌 이체" to="/transfer" />
                <SidebarItem icon={Banknote} label="출금 처리" to="/withdraw" />
                <SidebarItem icon={Search} label="계좌 조회" to="/inquiry" />
                <SidebarItem icon={UserCog} label="사용자 관리" to="/employee-management" />
            </nav>
        </aside>
    );
};

export default Sidebar;
