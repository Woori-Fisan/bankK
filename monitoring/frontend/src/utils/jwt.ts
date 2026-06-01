/**
 * JWT 토큰의 페이로드를 디코딩하여 JSON 객체로 반환합니다.
 */
export const decodeJwt = (token: string) => {
    try {
        const base64Url = token.split('.')[1]; // 페이로드 부분 추출
        if (!base64Url) return null;

        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
            window.atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(jsonPayload);
    } catch (error) {
        console.error('JWT 디코딩 실패:', error);
        return null;
    }
};
