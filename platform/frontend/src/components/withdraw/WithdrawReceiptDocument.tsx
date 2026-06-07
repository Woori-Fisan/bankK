import React from 'react';
import { formatDate } from '../../utils/formatter';

interface WithdrawReceiptDocumentProps {
    innerRef: React.RefObject<HTMLDivElement | null>;
    transactionId: string;
    userName: string;
    bankName: string;
    accountNumber: string;
    amount: string;
    fee: number;
    dateTime: string;
    balanceBefore: number;
    balanceAfter: number;
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

const WithdrawReceiptDocument: React.FC<WithdrawReceiptDocumentProps> = ({
    innerRef, transactionId, userName, bankName, accountNumber,
    amount, fee, dateTime, balanceBefore, balanceAfter,
}) => {
    const today = new Date()
        .toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
        .replace(/\. /g, '.')
        .replace('.', '년 ')
        .replace('.', '월 ')
        .replace('.', '일');
    const maskedAccount = accountNumber
        ? `${accountNumber.slice(0, 3)}-***-***${accountNumber.slice(-3)}`
        : '';

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
                        {bankName}
                    </div>
                    <div style={{ fontSize: 20, fontWeight: 700, letterSpacing: 6, color: '#222', marginBottom: 16 }}>
                        출 금 확 인 서
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#666', maxWidth: 500, margin: '0 auto' }}>
                        <span>거래번호: {transactionId}</span>
                        <span>발급일: {today}</span>
                    </div>
                </div>

                {/* 고객 정보 */}
                <SectionTitle>■ 고객 정보</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('성    명', userName)}
                        {row('출금 계좌', `${bankName} ${maskedAccount}`)}
                    </tbody>
                </table>

                {/* 출금 내역 */}
                <SectionTitle>■ 출금 내역</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('출금 금액', `${Number(amount).toLocaleString()} 원`)}
                        {row('수  수  료', `${fee.toLocaleString()} 원`)}
                        {row('거래 일시', formatDate(dateTime, true, 'dot'))}
                        {row('거래 번호', transactionId)}
                    </tbody>
                </table>

                {/* 잔액 정보 */}
                <SectionTitle>■ 잔액 정보</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('출금 전 잔액', `${balanceBefore.toLocaleString()} 원`)}
                        {row('출금 후 잔액', `${balanceAfter.toLocaleString()} 원`)}
                    </tbody>
                </table>

                {/* 푸터 */}
                <div style={{ borderTop: '1px solid #ccc', paddingTop: 24, fontSize: 12, color: '#888', lineHeight: 1.8 }}>
                    <p style={{ margin: 0 }}>본 확인서는 출금 거래를 증명하는 공식 문서입니다.</p>
                    <p style={{ margin: 0 }}>거래 관련 문의: 우리은행 고객센터 1588-5000</p>
                    <div style={{ textAlign: 'right', marginTop: 40, fontSize: 14, color: '#333', fontWeight: 600 }}>
                        {bankName}장 &nbsp;&nbsp; (인)
                    </div>
                </div>
            </div>
        </div>
    );
};

export default WithdrawReceiptDocument;
