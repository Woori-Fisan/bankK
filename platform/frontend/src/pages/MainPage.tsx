import React from 'react';
import {
    Landmark,
    ArrowRightLeft,
    Search,
    Banknote,
    Users,
    UserCog,
} from 'lucide-react';
import ServiceCard from '../components/common/ServiceCard';

const MainPage: React.FC = () => {
    return (
        <div className="flex-1 overflow-auto px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">Service Hub (Main)</h1>
                <p className="text-gray-600">Select a core service module to initiate an application or inquiry.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                <ServiceCard
                    icon={Landmark}
                    bgIcon={Landmark}
                    title="Loan Management"
                    description="Initiate institutional loan applications, review underwriting status, and manage existing credit facilities."
                />
                <ServiceCard
                    icon={ArrowRightLeft}
                    bgIcon={ArrowRightLeft}
                    title="Transfers"
                    description="Execute high-volume wire transfers, ACH batches, and internal ledger movements with strict maker-checker controls."
                />
                <ServiceCard
                    icon={Search}
                    bgIcon={Search}
                    title="Account Inquiry"
                    description="Deep dive into corporate account structures, view real-time balances, and access historical transaction ledgers."
                />
                <ServiceCard
                    icon={Banknote}
                    bgIcon={Banknote}
                    title="Withdrawal processing"
                    description="Manage large-scale fiat withdrawals, coordinate liquidity pools, and process physical currency requests."
                />
                <ServiceCard
                    icon={Users}
                    bgIcon={UserCog}
                    title="User Directory"
                    description="Administrate platform access, configure RBAC (Role-Based Access Control) profiles, and audit user activity logs."
                />
            </div>
        </div>
    );
};

export default MainPage;
