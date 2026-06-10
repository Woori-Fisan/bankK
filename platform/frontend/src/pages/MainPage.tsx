import React from 'react';
import {
    Landmark,
    ArrowRightLeft,
    Search,
    Banknote,
    Users,
} from 'lucide-react';
import ServiceCard from '../components/common/ServiceCard';
import PageHeader from '../components/common/PageHeader';
import Section from '../components/common/Section';
import { useAuth } from '../hooks/useAuth';

const MainPage: React.FC = () => {
    const { userId, isAdmin } = useAuth();

    return (
        <div className="flex-1 overflow-y-auto bg-gray-50/50">
            <div className="max-w-7xl mx-auto px-10 py-12 w-full min-h-full flex flex-col">
                <PageHeader
                title={`안녕하세요, ${userId}님`}
                description="진행하실 금융 업무 또는 조회 서비스를 선택해 주세요."
            />

            <Section title="주요업무 바로가기" columns={2} gap={6}>
                <ServiceCard
                    icon={Landmark}
                    title="대출 관리"
                    description="대출 신청, 심사 상태 조회 및 기존 대출 상품을 관리합니다."
                    to="/loan"
                    iconBg="bg-green-100"
                    iconColor="text-green-600"
                    arrowBg="bg-green-50"
                    arrowHoverBg="group-hover:bg-green-100"
                    arrowColor="text-green-400"
                />
                <ServiceCard
                    icon={ArrowRightLeft}
                    title="계좌 이체"
                    description="대량 자금 이체, 타행 송금 내역을 처리합니다."
                    to="/transfer"
                    iconBg="bg-blue-100"
                    iconColor="text-blue-500"
                    arrowBg="bg-blue-50"
                    arrowHoverBg="group-hover:bg-blue-100"
                    arrowColor="text-blue-400"
                />
                <ServiceCard
                    icon={Search}
                    title="계좌 조회"
                    description="계좌의 실시간 잔액과 상세 거래 내역을 확인합니다."
                    to="/inquiry"
                    iconBg="bg-indigo-100"
                    iconColor="text-indigo-500"
                    arrowBg="bg-indigo-50"
                    arrowHoverBg="group-hover:bg-indigo-100"
                    arrowColor="text-indigo-400"
                />
                <ServiceCard
                    icon={Banknote}
                    title="출금 처리"
                    description="현금 출금 요청을 접수하고 처리 상태를 확인합니다."
                    to="/withdraw"
                    iconBg="bg-orange-100"
                    iconColor="text-orange-500"
                    arrowBg="bg-orange-50"
                    arrowHoverBg="group-hover:bg-orange-100"
                    arrowColor="text-orange-400"
                />
                {isAdmin && (
                    <ServiceCard
                        icon={Users}
                        title="사용자 관리"
                        description="직원 등록, 조회, 비밀번호 변경과 같은 직원 관리 기능을 제공합니다."
                        to="/employee-management"
                        iconBg="bg-violet-100"
                        iconColor="text-violet-500"
                        arrowBg="bg-violet-50"
                        arrowHoverBg="group-hover:bg-violet-100"
                        arrowColor="text-violet-400"
                    />
                )}
            </Section>
        </div>
    </div>
);
};

export default MainPage;
