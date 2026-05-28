import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import {
    LayoutDashboard,
    Activity,
    Landmark,
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
                    ? 'bg-slate-700/50 border-l-4 border-emerald-500 text-white font-bold'
                    : 'text-white/80 hover:text-white hover:bg-slate-800/50'
            }`
        }
    >
        <Icon className="w-5 h-5" />
        <span className="text-sm font-medium">{label}</span>
    </NavLink>
);

const Sidebar: React.FC = () => {
    return (
        <aside className="w-60 bg-slate-900 flex flex-col flex-shrink-0">
            <div className="p-6 pb-8">
                <Link to="/" className="flex items-center gap-3 cursor-pointer group">
                    <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center transition-transform group-hover:scale-105 shadow-sm">
                        <Landmark className="w-6 h-6 text-emerald-600" />
                    </div>
                    <div>
                        <h1 className="text-white text-2xl font-bold leading-tight text-left">BankBridge</h1>
                        <p className="text-white/60 text-[10px] text-left uppercase tracking-wider font-semibold">Monitoring System</p>
                    </div>
                </Link>
            </div>

            <nav className="flex-1">
                <SidebarItem icon={LayoutDashboard} label="시스템 대시보드" to="/dashboard" />
                <SidebarItem icon={Activity} label="실시간 메트릭" to="/metrics" />
            </nav>
        </aside>
    );
};

export default Sidebar;
