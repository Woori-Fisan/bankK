/**
 * 금액을 세 자리마다 콤마(,)가 있는 형식으로 포맷팅합니다.
 * @param {number|string} amount - 포맷팅할 금액
 * @returns {string} 포맷팅된 금액 문자열
 */
export const formatAmount = (amount: number | string) => {
    if (!amount && amount !== 0) return '';
    return Number(amount).toLocaleString('ko-KR');
};

/**
 * 계좌번호에 하이픈을 추가하거나 마스킹 처리를 할 때 사용할 수 있는 유틸리티입니다.
 * (필요에 따라 구현을 확장할 수 있습니다.)
 */
export const formatAccountNumber = (accNo: string) => {
    if (!accNo) return '';
    return accNo; // 기본적으로는 그대로 반환하되, 필요시 정규식 적용
};
