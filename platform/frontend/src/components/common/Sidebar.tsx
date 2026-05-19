import React from 'react';
import { NavLink } from 'react-router-dom';
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
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center">
                        <Landmark className="w-6 h-6 text-slate-900" />
                    </div>
                    <div>
                        <h1 className="text-white text-xl font-bold leading-tight">Bank Bridge</h1>
                        <p className="text-slate-400 text-xs">Institutional Banking</p>
                    </div>
                </div>
            </div>

            <nav className="flex-1">
                <SidebarItem icon={LayoutDashboard} label="Dashboard" to="/" />
                <SidebarItem icon={ClipboardList} label="Loan" to="/loan" />
                <SidebarItem icon={ArrowLeftRight} label="Transfer" to="/transfer" />
                <SidebarItem icon={Banknote} label="Withdrawal" to="/withdraw" />
                <SidebarItem icon={Search} label="Account Inquiry" to="/inquiry" />
                <SidebarItem icon={UserCog} label="User Management" to="/users" />
            </nav>
        </aside>
    );
};

export default Sidebar;
