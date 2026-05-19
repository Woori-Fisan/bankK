/**
 * 계좌번호 형식이 유효한지 검사합니다.
 * @param {string} value - 계좌번호
 * @returns {boolean} 유효 여부
 */
export const isValidAccountNumber = (value) => {
    return /^\d{10,14}$/.test(value.replace(/-/g, ''));
};

/**
 * 금액이 유효한지 검사합니다. (0보다 큰 숫자)
 * @param {string|number} value - 금액
 * @returns {boolean} 유효 여부
 */
export const isValidAmount = (value) => {
    const num = parseInt(value, 10);
    return !isNaN(num) && num > 0;
};
