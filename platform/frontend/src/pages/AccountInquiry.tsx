import React, { useState } from 'react';
import BalanceSummary from '../components/account/BalanceSummary';
import TransactionFilter from '../components/account/TransactionFilter';
import type { FilterState } from '../components/account/TransactionFilter';
import TransactionTable from '../components/account/TransactionTable';
import type { Transaction } from '../components/account/TransactionTable';

const allDummyTransactions: Transaction[] = [
    { id: '1', date: '2023-10-24 14:32:10', description: '한국은행 타행환결제', withdrawal: 85000000, deposit: null, balance: 24592840000, status: '완료' },
    { id: '2', date: '2023-10-24 11:15:00', description: '위탁기금 운용수익 입금', withdrawal: null, deposit: 120500000, balance: 24677840000, status: '완료' },
    { id: '3', date: '2023-10-23 16:45:22', description: '수수료 정산 (10월분)', withdrawal: 2340000, deposit: null, balance: 24557340000, status: '대기' },
    { id: '4', date: '2023-10-23 09:00:05', description: '국고채 이자 수취', withdrawal: null, deposit: 45000000, balance: 24559680000, status: '완료' },
    { id: '5', date: '2023-10-22 13:20:45', description: 'B기관 대출 실행 (출금)', withdrawal: 500000000, deposit: null, balance: 24514680000, status: '완료' },
    { id: '6', date: '2023-10-21 10:00:00', description: '법인세 납부', withdrawal: 120000000, deposit: null, balance: 24394680000, status: '완료' },
    { id: '7', date: '2023-10-20 15:30:00', description: 'C기관 이자 입금', withdrawal: null, deposit: 15000000, balance: 24409680000, status: '완료' },
    { id: '8', date: '2023-10-19 09:20:00', description: '급여 이체 (10월)', withdrawal: 850000000, deposit: null, balance: 23559680000, status: '완료' },
    { id: '9', date: '2023-10-18 14:10:00', description: '임대료 자동이체', withdrawal: 45000000, deposit: null, balance: 23514680000, status: '완료' },
    { id: '10', date: '2023-10-17 11:45:00', description: '운용 자산 배당금', withdrawal: null, deposit: 320000000, balance: 23834680000, status: '완료' },
    { id: '11', date: '2023-10-16 16:00:00', description: '통신비 납부', withdrawal: 1200000, deposit: null, balance: 23833480000, status: '완료' },
    { id: '12', date: '2023-10-15 10:30:00', description: '예비비 입금', withdrawal: null, deposit: 50000000, balance: 23883480000, status: '완료' },
    { id: '13', date: '2023-10-14 13:50:00', description: '사무실 유지보수비', withdrawal: 8500000, deposit: null, balance: 23874980000, status: '완료' },
    { id: '14', date: '2023-10-13 09:15:00', description: 'D기관 채권 이자', withdrawal: null, deposit: 22000000, balance: 23896980000, status: '완료' },
    { id: '15', date: '2023-10-12 15:40:00', description: '소모품 구매', withdrawal: 3400000, deposit: null, balance: 23893580000, status: '완료' },
    { id: '16', date: '2023-10-11 11:20:00', description: '해외 송금 수수료', withdrawal: 550000, deposit: null, balance: 23893030000, status: '완료' },
    { id: '17', date: '2023-10-10 14:00:00', description: '보안 시스템 업그레이드', withdrawal: 15000000, deposit: null, balance: 23878030000, status: '완료' },
    { id: '18', date: '2023-10-09 10:10:00', description: '신규 프로젝트 투자금', withdrawal: 2000000000, deposit: null, balance: 21878030000, status: '완료' },
    { id: '19', date: '2023-10-08 16:25:00', description: '정기 예금 만기 입금', withdrawal: null, deposit: 1000000000, balance: 22878030000, status: '완료' },
    { id: '20', date: '2023-10-07 09:40:00', description: '전기요금 납부', withdrawal: 2450000, deposit: null, balance: 22875580000, status: '완료' },
    { id: '21', date: '2023-10-06 13:15:00', description: '수도요금 납부', withdrawal: 850000, deposit: null, balance: 22874730000, status: '완료' },
    { id: '22', date: '2023-10-05 11:00:00', description: '자산 관리 수수료', withdrawal: 12000000, deposit: null, balance: 22862730000, status: '완료' },
    { id: '23', date: '2023-10-04 15:20:00', description: 'E기관 펀드 환매', withdrawal: null, deposit: 500000000, balance: 23362730000, status: '완료' },
    { id: '24', date: '2023-10-03 10:50:00', description: '사무용 가구 교체', withdrawal: 25000000, deposit: null, balance: 23337730000, status: '완료' },
    { id: '25', date: '2023-10-02 14:30:00', description: '홍보물 제작비', withdrawal: 4500000, deposit: null, balance: 23333230000, status: '완료' },
];

const AccountInquiry: React.FC = () => {
    const [filteredTransactions, setFilteredTransactions] = useState<Transaction[]>(allDummyTransactions);
    const [currentPage, setCurrentPage] = useState(1);
    const pageSize = 5;

    const handleSearch = (filters: FilterState) => {
        let result = [...allDummyTransactions];

        if (filters.keyword) {
            result = result.filter(tx => 
                tx.description.includes(filters.keyword)
            );
        }

        if (filters.type === '입금') {
            result = result.filter(tx => tx.deposit !== null);
        } else if (filters.type === '출금') {
            result = result.filter(tx => tx.withdrawal !== null);
        }

        result = result.filter(tx => {
            const txDate = tx.date.split(' ')[0];
            return txDate >= filters.startDate && txDate <= filters.endDate;
        });

        setFilteredTransactions(result);
        setCurrentPage(1); // 검색 시 첫 페이지로 이동
    };

    const handleReset = () => {
        setFilteredTransactions(allDummyTransactions);
        setCurrentPage(1);
    };

    const handlePageChange = (page: number) => {
        setCurrentPage(page);
    };

    // 현재 페이지에 해당하는 데이터만 추출
    const paginatedTransactions = filteredTransactions.slice(
        (currentPage - 1) * pageSize,
        currentPage * pageSize
    );

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-full flex flex-col bg-gray-50">
            <header className="mb-8">
                <h1 className="text-2xl font-bold text-gray-900">거래 내역 및 잔액 조회</h1>
                <p className="text-gray-500 text-sm mt-1">실시간 계좌 잔액과 상세 거래 내역을 확인할 수 있습니다.</p>
            </header>

            <BalanceSummary />
            
            <section className="flex-1 flex flex-col">
                <TransactionFilter onSearch={handleSearch} onReset={handleReset} />
                <TransactionTable 
                    transactions={paginatedTransactions} 
                    currentPage={currentPage}
                    totalEntries={filteredTransactions.length}
                    pageSize={pageSize}
                    onPageChange={handlePageChange}
                />
            </section>
        </div>
    );
};

export default AccountInquiry;
