import { importSPKI, CompactEncrypt } from 'jose';
import axiosInstance from '../api/axiosInstance';
import { useBankKeyStore } from '../store/useBankKeyStore';
import { createJwsSignature } from './authCrypto';

/**
 * 모든 은행의 최신 RSA 공개키를 서버로부터 조회하여 Zustand 저장소에 업데이트합니다.
 */
export const refreshBankPublicKeys = async (): Promise<void> => {
    try {
        const response = await axiosInstance.get('/keys/public');
        
        // 백엔드 ApiResponse 구조: response.data.data 에 Map<String, BankRsaKeyResponse> 가 들어있음
        const keys = response.data?.data;
        
        if (keys) {
            useBankKeyStore.getState().setAllBankKeys(keys);
            console.log('모든 은행의 공개키가 성공적으로 갱신되었습니다.', keys);
        }
    } catch (error) {
        console.error('은행 공개키 갱신 실패:', error);
    }
};

/**
 * 하이브리드 암호화 (디지털 봉투) 수행
 * 1. 일회용 AES 대칭키 생성 및 민감 데이터 암호화 (A256GCM)
 * 2. 해당 AES 대칭키를 은행의 RSA 공개키로 암호화 (RSA-OAEP-256)
 * 
 * @param sensitiveData 암호화할 민감 정보 객체
 * @param bankCode 대상 은행 코드
 * @returns { reqPayload: string } | null (전체 JWE 문자열)
 */
export const hybridEncrypt = async (
    sensitiveData: object,
    bankCode: string
): Promise<{ reqPayload: string } | null> => {
    try {
        // 1. 저장소에서 해당 은행의 공개키 가져오기
        const publicKeyPem = useBankKeyStore.getState().getBankPublicKey(bankCode);

        if (!publicKeyPem) {
            console.error(`은행[${bankCode}]의 공개키를 찾을 수 없습니다. 키 갱신이 필요합니다.`);
            return null;
        }

        // 2. PEM 문자열을 CryptoKey 객체로 변환
        const publicKey = await importSPKI(publicKeyPem, 'RSA-OAEP-256');

        // 3. 민감 데이터를 바이트 배열로 변환
        const dataBytes = new TextEncoder().encode(JSON.stringify(sensitiveData));

        // 4. jose의 CompactEncrypt를 사용하여 하이브리드 암호화 수행
        // Compact JWE 형식: header.encryptedKey.iv.ciphertext.tag
        const jwe = await new CompactEncrypt(dataBytes)
            .setProtectedHeader({ alg: 'RSA-OAEP-256', enc: 'A256GCM' })
            .encrypt(publicKey);

        // 5. 암호문 덩어리 전체(JWE)를 reqPayload로 반환 (Zero-Knowledge Pass-through)
        return {
            reqPayload: jwe
        };
    } catch (error) {
        console.error('하이브리드 암호화 과정 중 오류 발생:', error);
        return null;
    }
};

/**
 * 보안 요청 준비 (암호화 + 서명 통합)
 * 1. 은행별 Key ID 조회
 * 2. 민감 데이터 하이브리드 암호화 (reqPayload 생성)
 * 3. JWS 전자서명 생성 (헤더용)
 * 
 * @param sensitiveData 암호화할 민감 정보 객체
 * @param bankCode 대상 은행 코드
 * @returns { payload: { reqPayload: string }, headers: { 'x-jws-signature': string, 'x-bank-key-id': string } } | null
 */
export const prepareSecureRequest = async (
    sensitiveData: object,
    bankCode: string
) => {
    try {
        // 1. 키 ID 조회
        const keyId = useBankKeyStore.getState().getBankKeyId(bankCode);
        if (!keyId) throw new Error(`은행[${bankCode}]의 키 ID를 찾을 수 없습니다.`);

        // 2. 하이브리드 암호화 수행
        const encryptionResult = await hybridEncrypt(sensitiveData, bankCode);
        if (!encryptionResult) throw new Error('데이터 암호화 실패');

        const { reqPayload } = encryptionResult;

        // 3. JWS 서명 생성 (페이로드에 암호문과 은행코드, 타임스탬프 포함)
        const jwsSignature = await createJwsSignature({
            reqPayload,
            depositBankCode: bankCode,
            timestamp: Date.now()
        });
        if (!jwsSignature) throw new Error('JWS 서명 생성 실패');

        // 4. API 호출에 즉시 사용 가능한 구조로 반환
        return {
            payload: { reqPayload },
            headers: {
                'x-jws-signature': jwsSignature,
                'x-bank-key-id': keyId
            }
        };
    } catch (error) {
        console.error('보안 요청 준비 중 오류 발생:', error);
        return null;
    }
};
