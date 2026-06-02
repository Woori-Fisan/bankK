import React from 'react';

interface LoanReceiptDocumentProps {
    innerRef: React.RefObject<HTMLDivElement | null>;
    loanId: string;
    borrowerName: string;
    executeAmount: number;
    interestRate: number;
    repaymentPeriod: number;
    monthlyPayment: number;
    repaymentStartDate: string;
    maturityDate: string;
    bank: string;
    accountNo: string;
    productName: string;
}

const row = (label: string, value: string) => (
    <tr key={label}>
        <td style={{ padding: '10px 20px', width: 140, background: '#f4f6fb', color: '#444', fontWeight: 600, borderBottom: '1px solid #e5e8ef', whiteSpace: 'nowrap' }}>
            {label}
        </td>
        <td style={{ padding: '10px 20px', color: '#111', borderBottom: '1px solid #e5e8ef' }}>
            {value}
        </td>
    </tr>
);

const SectionTitle: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <div style={{ fontSize: 14, fontWeight: 700, color: '#1a2e5a', background: '#eef1f8', padding: '8px 20px', marginBottom: 0, borderLeft: '4px solid #1a2e5a' }}>
        {children}
    </div>
);

const LoanReceiptDocument: React.FC<LoanReceiptDocumentProps> = ({
    innerRef, loanId, borrowerName,
    executeAmount, interestRate, repaymentPeriod, monthlyPayment,
    repaymentStartDate, maturityDate, bank, accountNo, productName,
}) => {
    const today = new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\. /g, '.').replace('.', '년 ').replace('.', '월 ').replace('.', '일');
    const maskedAccount = accountNo ? `${accountNo.slice(0, 3)}-***-***${accountNo.slice(-3)}` : '';

    return (
        <div style={{ position: 'fixed', left: -9999, top: 0, zIndex: -1 }}>
            <div
                ref={innerRef}
                style={{
                    width: 794,
                    minHeight: 1123,
                    background: '#ffffff',
                    padding: '60px 64px 80px',
                    fontFamily: 'Pretendard, "Noto Sans KR", sans-serif',
                    fontSize: 14,
                    color: '#111',
                    boxSizing: 'border-box',
                }}
            >
                {/* 헤더 */}
                <div style={{ textAlign: 'center', paddingBottom: 28, marginBottom: 32, borderBottom: '2px solid #1a2e5a' }}>
                    <div style={{ fontSize: 26, fontWeight: 900, letterSpacing: 10, color: '#1a2e5a', marginBottom: 6 }}>
                        우 리 은 행
                    </div>
                    <div style={{ fontSize: 20, fontWeight: 700, letterSpacing: 6, color: '#222', marginBottom: 16 }}>
                        대 출 실 행 확 인 서
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#666', maxWidth: 500, margin: '0 auto' }}>
                        <span>대출번호: {loanId}</span>
                        <span>발급일: {today}</span>
                    </div>
                </div>

                {/* 고객 정보 */}
                <SectionTitle>■ 고객 정보</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('성    명', borrowerName)}
                        {row('입금 계좌', `${bank} ${maskedAccount}`)}
                    </tbody>
                </table>

                {/* 대출 내역 */}
                <SectionTitle>■ 대출 내역</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('대출 상품', productName)}
                        {row('대출 금액', `${executeAmount.toLocaleString()} 원`)}
                        {row('적용 금리', `연 ${interestRate}% (고정금리)`)}
                        {row('대출 기간', `${repaymentPeriod}개월`)}
                    </tbody>
                </table>

                {/* 상환 정보 */}
                <SectionTitle>■ 상환 정보</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('상환 방법', '원리금균등상환')}
                        {row('월 상환금', `${monthlyPayment.toLocaleString()} 원`)}
                        {row('최초 상환일', repaymentStartDate)}
                        {row('만  기  일', maturityDate)}
                    </tbody>
                </table>

                {/* 푸터 */}
                <div style={{ borderTop: '1px solid #ccc', paddingTop: 24, fontSize: 12, color: '#888', lineHeight: 1.8 }}>
                    <p style={{ margin: 0 }}>본 확인서는 대출 실행을 증명하는 공식 문서입니다.</p>
                    <p style={{ margin: 0 }}>대출 관련 문의: 우리은행 고객센터 1588-5000</p>
                    <div style={{ textAlign: 'right', marginTop: 40, fontSize: 14, color: '#333', fontWeight: 600 }}>
                        우 리 은 행 장 &nbsp;&nbsp; (인)
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LoanReceiptDocument;
