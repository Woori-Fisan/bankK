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
import PageHeader from '../components/common/PageHeader';
import Section from '../components/common/Section';
import { useAuth } from '../hooks/useAuth';

const MainPage: React.FC = () => {
    const { isAdmin } = useAuth();

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50">
            <div className="max-w-7xl mx-auto px-10 py-12 w-full min-h-full flex flex-col">
                <PageHeader 
                title="서비스 허브"
                description="진행하실 금융 업무 또는 조회 서비스를 선택해 주세요."
            />

            <Section columns={2} gap={8}>
                <ServiceCard
                    icon={Landmark}
                    bgIcon={Landmark}
                    title="대출 관리"
                    description="기관 대출 신청, 심사 상태 조회 및 기존 대출 상품을 관리합니다."
                    to="/loan"
                />
                <ServiceCard
                    icon={ArrowRightLeft}
                    bgIcon={ArrowRightLeft}
                    title="계좌 이체"
                    description="대량 자금 이체, 타행 송금 내부."
                    to="/transfer"
                />
                <ServiceCard
                    icon={Search}
                    bgIcon={Search}
                    title="계좌 조회"
                    description="계좌의 실시간 잔액을 확인하고 상세한 과거 거래 내역을 조회합니다."
                    to="/inquiry"
                />
                <ServiceCard
                    icon={Banknote}
                    bgIcon={Banknote}
                    title="출금 처리"
                    description="현금 출금 요청을 처리합니다."
                    to="/withdraw"
                />
                {isAdmin && (
                    <ServiceCard
                        icon={Users}
                        bgIcon={UserCog}
                        title="사용자 관리"
                        description="직원 등록, 조회, 비밀번호 변경과 같은 직원 관리 기능을 제공합니다."
                        to="/employee-management"
                    />
                )}
            </Section>
        </div>
    </div>
);
};

export default MainPage;
