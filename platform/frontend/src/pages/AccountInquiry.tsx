import React, { useState } from 'react';
import { AlertCircle } from 'lucide-react';
import BalanceSummary from '../components/account/BalanceSummary';
import TransactionFilter from '../components/account/TransactionFilter';
import type { FilterState } from '../components/account/TransactionFilter';
import TransactionTable from '../components/account/TransactionTable';
import type { Transaction } from '../components/account/TransactionTable';
import AccountInputStep from '../components/account/AccountInputStep';
import { fetchBalance, fetchTransactionHistory } from '../api/inquiry';

const AccountInquiry: React.FC = () => {
    // 1. 단계 관리 상태 (1: 정보 입력, 2: 조회 결과)
    const [step, setStep] = useState(1);
    const [accountInfo, setAccountInfo] = useState<any>(null);
    const [apiError, setApiError] = useState('');
    
    // API 데이터 상태
    const [balanceData, setBalanceData] = useState<{ balance: string; status: string } | null>(null);
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [totalItems, setTotalItems] = useState(0); 
    const [totalPages, setTotalPages] = useState(0); // 백엔드에서 주는 전체 페이지 수
    const [isLoading, setIsLoading] = useState(false);
    
    // 페이지네이션 및 필터 상태
    const [currentPage, setCurrentPage] = useState(1);
    const pageSize = 20;
    const [currentFilters, setCurrentFilters] = useState<FilterState>({
        startDate: new Date(new Date().setMonth(new Date().getMonth() - 1)).toISOString().split('T')[0],
        endDate: new Date().toISOString().split('T')[0]
    });
    // 공통 거래 내역 호출 함수 (초기 로딩, 검색, 페이지 이동 시 재사용)
    const loadTransactions = async (info: any, filters: FilterState, page: number) => {
        try {
            const historyRes = await fetchTransactionHistory({
                ...info,
                startDate: filters.startDate,
                endDate: filters.endDate,
                page: page - 1, // 백엔드(0-based) 규격에 맞춰 -1 처리
                size: pageSize // 일관된 사이즈 사용
            });

            if (historyRes.success && historyRes.data?.history) {
                const mappedTransactions: Transaction[] = historyRes.data.history.map((h: any) => ({
                    id: h.txId,
                    date: h.txDate,
                    description: h.description,
                    target: h.counterpartName || null, // 백엔드 DTO(counterpartName) 필드로 매핑 수정
                    type: h.txType,
                    amount: h.amount,
                    balance: h.balance,
                    status: '완료'
                }));
                setTransactions(mappedTransactions);

                setTotalItems(historyRes.data.totalCount || 0);
                setTotalPages(historyRes.data.totalPages || 0);
            } else if (!historyRes.success) {
                throw new Error(historyRes.error?.message || '거래 내역을 불러오지 못했습니다.');
            }
        } catch (err) {
            console.error('Transaction Load Error:', err);
            throw err; // 에러를 상위(handleNextStep)로 던져서 공통 처리
        }
    };

    // 1단계 -> 2단계 넘어올 때 초기 데이터 로딩
    const handleNextStep = async (data: any) => {
        setAccountInfo(data);
        setIsLoading(true);
        setApiError(''); // 새로운 요청 시 기존 에러 초기화
        
        const defaultFilters: FilterState = {
            startDate: new Date(new Date().setMonth(new Date().getMonth() - 1)).toISOString().split('T')[0],
            endDate: new Date().toISOString().split('T')[0],
        };

        // [수정] 새로운 조회를 위해 페이지와 필터 상태를 초기화
        setCurrentPage(1);
        setCurrentFilters(defaultFilters);

        try {
            // 잔액 조회
            const balanceRes = await fetchBalance(data);
            if (!balanceRes.success) {
                throw new Error(balanceRes.error?.message || '잔액 정보를 불러오지 못했습니다.');
            }
            setBalanceData(balanceRes.data);
            
            // 거래 내역 조회 (초기 로딩 - 초기화된 필터값 사용)
            await loadTransactions(data, defaultFilters, 1);
            
            // 두 API가 모두 성공해야만 다음 단계로 넘어감
            setStep(2);
        } catch (err: any) {
            console.error('Account Inquiry Error:', err);
            setApiError(err.message || '계좌 정보를 불러오는 중 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    // 필터에서 '검색' 버튼 눌렀을 때
    const handleSearch = async (filters: FilterState) => {
        setIsLoading(true);
        setApiError(''); // 검색 시작 시 기존 에러 초기화
        setCurrentFilters(filters); // 검색 조건 저장
        setCurrentPage(1); // 검색 시 무조건 1페이지로 리셋
        try {
            await loadTransactions(accountInfo, filters, 1);
        } catch (err: any) {
            console.error('Search Error:', err);
            setApiError(err.message || '거래 내역 검색 중 오류가 발생했습니다.');
        }
        setIsLoading(false);
    };

    // 페이지 번호 눌렀을 때
    const handlePageChange = async (page: number) => {
        setIsLoading(true);
        setApiError(''); // 페이지 이동 시 기존 에러 초기화
        setCurrentPage(page);
        try {
            // 저장해둔 조건과 새로운 페이지 번호로 재요청
            await loadTransactions(accountInfo, currentFilters, page);
        } catch (err: any) {
            console.error('Page Change Error:', err);
            setApiError(err.message || '페이지 이동 중 오류가 발생했습니다.');
        }
        setIsLoading(false);
    };

    const handleReset = () => {
        setStep(1); // 초기 입력 단계로 리셋
        setApiError('');
    };

    // Step 1: 입력 화면
    if (step === 1) {
        return <AccountInputStep 
            onNext={handleNextStep} 
            apiError={apiError} 
            clearApiError={() => setApiError('')} 
        />;
    }

    // Step 2: 결과 화면
    return (
        <div className={`p-8 max-w-7xl mx-auto min-h-full flex flex-col bg-gray-50 transition-opacity ${isLoading ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            <header className="mb-8 flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">거래 내역 및 잔액 조회</h1>
                    <p className="text-slate-500 text-sm mt-1">
                        [{accountInfo?.accountNo}] 계좌의 거래 내역입니다.
                    </p>
                </div>
                <button 
                    onClick={handleReset}
                    className="text-sm font-bold text-emerald-600 hover:text-emerald-700 transition-colors"
                >
                    다른 계좌 조회하기
                </button>
            </header>

            {/* Step 2 에러 배너 추가 */}
            {apiError && (
                <div className="mb-6 flex items-center gap-2 text-rose-500 bg-rose-50 p-4 rounded-2xl border border-rose-100">
                    <AlertCircle className="w-5 h-5 flex-shrink-0" />
                    <span className="text-sm font-bold">{apiError}</span>
                </div>
            )}

            <BalanceSummary customBalance={balanceData?.balance} />
            
            <section className="flex-1 flex flex-col">
                <TransactionFilter onSearch={handleSearch} onReset={handleReset} />
                <TransactionTable 
                    transactions={transactions} // 더 이상 slice()로 자르지 않고 받아온 그대로 넘김
                    currentPage={currentPage}
                    totalEntries={totalItems} // 백엔드에서 받은 전체 개수 전달
                    totalPages={totalPages}   // 백엔드에서 받은 전체 페이지 수 전달
                    onPageChange={handlePageChange}
                />
            </section>
        </div>
    );
};

export default AccountInquiry;
