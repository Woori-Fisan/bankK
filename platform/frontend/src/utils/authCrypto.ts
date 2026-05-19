import { importSPKI, importPKCS8, CompactEncrypt, CompactSign } from 'jose';

// 플랫폼 RSA 공개키를 가져오는 함수 (테스트용 더미)
export const fetchPlatformPublicKey = async (): Promise<string> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    return `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyYt9L2K6pD8h5t2F2c6M
9zX7P5tD7c4M4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j3D
-----END PUBLIC KEY-----`;
};

// 플랫폼 비밀번호 RSA 암호화 함수
export const encryptPassword = async (password: string, publicKeyPem: string): Promise<string | null> => {
    try {
        // 브라우저 네이티브 Web Crypto API가 파싱할 수 있게 공개키 포맷 import
        const publicKey = await importSPKI(publicKeyPem, 'RSA-OAEP-256');
        
        // 평문 데이터를 Uint8Array 바이트 배열로 인코딩
        const textBytes = new TextEncoder().encode(password);
        
        // 최신 jose Spec인 CompactEncrypt 클래스 인스턴스 생성 및 실행
        const jwe = await new CompactEncrypt(textBytes)
            .setProtectedHeader({ alg: 'RSA-OAEP-256', enc: 'A256GCM' })
            .encrypt(publicKey);
            
        return jwe; // ey... 로 시작하는 암호화 문자열 리턴
    } catch (e) {
        console.error("비밀번호 RSA-JWE 암호화 실패:", e);
        return null;
    }
};

// JWS 전자서명 생성 함수
export const createJwsSignature = async (payload: object, privateKeyPem: string): Promise<string | null> => {
    try {
        // 플랫폼 자체 개인키 파싱 (서명용)
        const privateKey = await importPKCS8(privateKeyPem, 'RS256');
        
        // JWS 페이로드는 항상 바이트 배열 형태여야 하므로 JSON 직렬화 후 인코딩
        const payloadBytes = new TextEncoder().encode(JSON.stringify(payload));
        
        // 최신 jose Spec인 CompactSign 클래스로 서명 도장 찍기
        const jws = await new CompactSign(payloadBytes)
            .setProtectedHeader({ alg: 'RS256' })
            .sign(privateKey);
            
        return jws;
    } catch (e) {
        console.error("JWS 전자서명 생성 실패:", e);
        return null;
    }
};