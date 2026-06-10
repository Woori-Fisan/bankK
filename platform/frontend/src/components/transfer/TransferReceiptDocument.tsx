import React from 'react';
import { formatAmount, formatDate } from '../../utils/formatter';

interface TransferReceiptDocumentProps {
    innerRef: React.RefObject<HTMLDivElement | null>;
    transactionId: string;
    fromName: string;
    fromBankName: string;
    fromAccountNumber: string;
    toName: string;
    toBankName: string;
    toBankAccountNo: string;
    amount: number;
    transactionDate: string;
    balanceAfter: string;
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

const mask = (accountNo: string) =>
    accountNo ? `${accountNo.slice(0, 3)}-***-***${accountNo.slice(-3)}` : '';

const TransferReceiptDocument: React.FC<TransferReceiptDocumentProps> = ({
    innerRef, transactionId,
    fromName, fromBankName, fromAccountNumber,
    toName, toBankName, toBankAccountNo,
    amount, transactionDate, balanceAfter,
}) => {
    const today = formatDate(new Date().toISOString(), false, 'text');

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
                        {fromBankName}
                    </div>
                    <div style={{ fontSize: 20, fontWeight: 700, letterSpacing: 6, color: '#222', marginBottom: 16 }}>
                        계 좌 이 체 확 인 서
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#666', maxWidth: 500, margin: '0 auto' }}>
                        <span>거래번호: {transactionId}</span>
                        <span>발급일: {today}</span>
                    </div>
                </div>

                {/* 송금인 정보 */}
                <SectionTitle>■ 송금인 정보</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('성    명', fromName)}
                        {row('출금 계좌', `${fromBankName} ${mask(fromAccountNumber)}`)}
                    </tbody>
                </table>

                {/* 수취인 정보 */}
                <SectionTitle>■ 수취인 정보</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('성    명', toName)}
                        {row('입금 계좌', `${toBankName} ${mask(toBankAccountNo)}`)}
                    </tbody>
                </table>

                {/* 이체 내역 */}
                <SectionTitle>■ 이체 내역</SectionTitle>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24, border: '1px solid #e5e8ef' }}>
                    <tbody>
                        {row('이체 금액', `${formatAmount(amount)} 원`)}
                        {row('거래 일시', transactionDate)}
                        {row('거래 번호', transactionId)}
                        {row('이체 후 잔액', `${formatAmount(balanceAfter)} 원`)}
                    </tbody>
                </table>

                {/* 푸터 */}
                <div style={{ borderTop: '1px solid #ccc', paddingTop: 24, fontSize: 12, color: '#888', lineHeight: 1.8 }}>
                    <p style={{ margin: 0 }}>본 확인서는 계좌이체 거래를 증명하는 공식 문서입니다.</p>
                    <p style={{ margin: 0 }}>거래 관련 문의: 우리은행 고객센터 1588-5000</p>
                    <div style={{ textAlign: 'right', marginTop: 40, fontSize: 14, color: '#333', fontWeight: 600 }}>
                        {fromBankName}장 &nbsp;&nbsp; (인)
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TransferReceiptDocument;
