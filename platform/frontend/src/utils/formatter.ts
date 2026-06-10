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
 * 금액을 한글 단위(만, 억 등)로 변환하여 반환합니다.
 * @param {number|string} amount - 변환할 금액
 * @returns {string} 한글 단위가 포함된 금액 문자열
 */
export const formatToKorean = (amount: number | string) => {
    const num = Number(amount);
    if (!num || isNaN(num)) return '';
    
    const units = ['', '만', '억', '조'];
    let result = '';
    let temp = num;
    let unitIdx = 0;
    
    while (temp > 0) {
        const part = temp % 10000;
        if (part > 0) {
            result = part.toLocaleString() + units[unitIdx] + ' ' + result;
        }
        temp = Math.floor(temp / 10000);
        unitIdx++;
    }
    
    return result.trim() + ' 원';
};

/**
 * 날짜 문자열을 다양한 형식으로 변환합니다.
 * @param dateStr - 변환할 날짜 문자열 (ISO 형식 등)
 * @param includeTime - 시간 포함 여부
 * @param type - 포맷 형식 ('text': 년/월/일, 'dot': YYYY.MM.DD, 'dash': YYYY-MM-DD)
 * @returns 포맷팅된 날짜 문자열
 */
export const formatDate = (dateStr: string, includeTime: boolean = false, type: 'text' | 'dot' | 'dash' = 'dot') => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');

    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    const s = String(date.getSeconds()).padStart(2, '0');

    if (type === 'text') {
        const base = `${y}년 ${Number(m)}월 ${Number(d)}일`;
        return includeTime ? `${base} ${Number(h)}시 ${Number(min)}분 ${Number(s)}초` : base;
    } else if (type === 'dash') {
        const base = `${y}-${m}-${d}`;
        return includeTime ? `${base} ${h}:${min}:${s}` : base;
    } else {
        const base = `${y}.${m}.${d}`;
        return includeTime ? `${base} ${h}:${min}:${s}` : base;
    }
};

