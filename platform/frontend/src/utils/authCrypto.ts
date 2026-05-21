import { importSPKI, CompactEncrypt, CompactSign } from 'jose';
import axios from 'axios';
import { getTerminalPrivateKey } from './terminalKeyStore';

// 플랫폼 비밀번호 RSA 암호화 함수 (서버에서 공개키를 직접 가져와서 암호화)
export const encryptPassword = async (password: string): Promise<string | null> => {
    try {
        // 1. 서버로부터 플랫폼 공개키 조회
        // 주의: axiosInstance를 쓰면 401 응답 시 인터셉터가 페이지를 강제 새로고침하므로 순수 axios 사용
        const response = await axios.get('/api/v1/auth/public-key');

        // 백엔드 ApiResponse 구조 지원: response.data.data.publicKey 또는 response.data.publicKey
        let publicKeyPem = response.data?.data?.publicKey || response.data?.publicKey;

        if (!publicKeyPem || typeof publicKeyPem !== 'string') {
            throw new Error('응답에 유효한 공개키(publicKey) 필드가 없습니다.');
        }

        // 리터럴 '\\n' 문자열이 들어왔을 경우 실제 줄바꿈 문자로 치환 및 공백 제거
        publicKeyPem = publicKeyPem.replace(/\\n/g, '\n').trim();

        // PEM 헤더가 없는 순수 Base64 문자열일 경우 포맷팅
        if (!publicKeyPem.includes('-----BEGIN PUBLIC KEY-----')) {
            publicKeyPem = `-----BEGIN PUBLIC KEY-----\n${publicKeyPem}\n-----END PUBLIC KEY-----`;
        }

        // 2. 브라우저 네이티브 Web Crypto API가 파싱할 수 있게 공개키 포맷 import
        const publicKey = await importSPKI(publicKeyPem, 'RSA-OAEP-256');

        // 3. 평문 데이터를 Uint8Array 바이트 배열로 인코딩
        const textBytes = new TextEncoder().encode(password);

        // 4. 최신 jose Spec인 CompactEncrypt 클래스 인스턴스 생성 및 실행
        const jwe = await new CompactEncrypt(textBytes)
            .setProtectedHeader({ alg: 'RSA-OAEP-256', enc: 'A256GCM' })
            .encrypt(publicKey);

        return jwe; // ey... 로 시작하는 암호화 문자열 리턴
    } catch (e) {
        console.error("비밀번호 RSA-JWE 암호화 실패:", e);
        return null;
    }
};

// JWS 전자서명 생성 함수 (IndexedDB의 개인키를 직접 사용)
export const createJwsSignature = async (payload: object): Promise<string | null> => {
    try {
        // 1. IndexedDB에서 단말기 개인키(CryptoKey) 조회
        const privateKey = await getTerminalPrivateKey();
        
        if (!privateKey) {
            console.error("단말기 개인키가 등록되어 있지 않습니다.");
            return null;
        }
        
        // 2. JWS 페이로드를 바이트 배열 형태로 인코딩
        const payloadBytes = new TextEncoder().encode(JSON.stringify(payload));
        
        // 3. 최신 jose Spec인 CompactSign 클래스로 서명 도장 찍기
        const jws = await new CompactSign(payloadBytes)
            .setProtectedHeader({ alg: 'RS256' })
            .sign(privateKey);
            
        return jws;
    } catch (e) {
        console.error("JWS 전자서명 생성 실패:", e);
        return null;
    }
};