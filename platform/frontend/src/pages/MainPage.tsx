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
                <h1 className="text-3xl font-bold text-gray-900 mb-2">서비스 허브</h1>
                <p className="text-gray-600">진행하실 금융 업무 또는 조회 서비스를 선택해 주세요.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
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
                    description="대량 자금 이체, 타행 송금 및 엄격한 승인 절차 기반의 내부 원장 이동을 실행합니다."
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
                    description="대규모 현금 출금 업무를 관리하고 유동성을 조정하며 물리적 화폐 요청을 처리합니다."
                    to="/withdraw"
                />
                <ServiceCard
                    icon={Users}
                    bgIcon={UserCog}
                    title="사용자 관리"
                    description="플랫폼 접근 권한을 관리하고 역할 기반 접근 제어(RBAC) 프로필을 설정 및 감사 로그를 확인합니다."
                    to="/employee-management"
                />
            </div>
        </div>
    );
};

export default MainPage;
