import { importSPKI } from 'jose';
import axiosInstance from '../api/axiosInstance';
import { useBankKeyStore } from '../store/useBankKeyStore';
import { createJwsSignature } from './authCrypto';

/**
 * Base64Url 인코딩 유틸리티 (바이너리 데이터를 안전하게 처리)
 */
const base64UrlEncode = (buffer: ArrayBuffer): string => {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
};

/**
 * Base64Url 디코딩 유틸리티
 */
const base64UrlDecode = (str: string): Uint8Array => {
    const base64 = str.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    return new Uint8Array(atob(padded).split('').map(c => c.charCodeAt(0)));
};

/**
 * 모든 은행의 최신 RSA 공개키를 서버로부터 조회하여 Zustand 저장소에 업데이트합니다.
 */
export const refreshBankPublicKeys = async (): Promise<void> => {
    try {
        const response = await axiosInstance.get('/keys/public');
        const keys = response.data?.data;
        if (keys) {
            useBankKeyStore.getState().setAllBankKeys(keys);
            console.log('모든 은행의 공개키가 성공적으로 갱신되었습니다.');
        }
    } catch (error) {
        console.error('은행 공개키 갱신 실패:', error);
    }
};

/**
 * 하이브리드 암호화 (디지털 봉투) 수행
 * JWE 표준(RFC 7516)에 따라 AAD(Additional Authenticated Data)를 포함합니다.
 */
export const hybridEncrypt = async (
    sensitiveData: object,
    bankCode: string
): Promise<{ reqPayload: string, aesKey: CryptoKey } | null> => {
    try {
        const publicKeyPem = useBankKeyStore.getState().getBankPublicKey(bankCode);
        if (!publicKeyPem) throw new Error(`은행[${bankCode}]의 공개키가 없습니다.`);

        // 1. JWE 헤더 생성 및 인코딩 (이 문자열이 AES-GCM의 AAD가 됨)
        const headerObj = { alg: 'RSA-OAEP-256', enc: 'A256GCM' };
        const header = base64UrlEncode(new TextEncoder().encode(JSON.stringify(headerObj)));

        // 2. AES-GCM 대칭키 생성
        const aesKey = await window.crypto.subtle.generateKey(
            { name: 'AES-GCM', length: 256 },
            true,
            ['encrypt', 'decrypt']
        );

        // 3. 민감 데이터 암호화 (AAD 포함)
        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const dataBytes = new TextEncoder().encode(JSON.stringify(sensitiveData));
        const encryptedData = await window.crypto.subtle.encrypt(
            { 
                name: 'AES-GCM', 
                iv, 
                additionalData: new TextEncoder().encode(header) // 중요: JWE 표준 AAD 적용
            },
            aesKey,
            dataBytes
        );

        const ciphertext = encryptedData.slice(0, -16);
        const authTag = encryptedData.slice(-16);

        // 4. AES 키(CEK)를 RSA 공개키로 암호화
        const publicKey = await importSPKI(publicKeyPem, 'RSA-OAEP-256');
        const exportedAesKey = await window.crypto.subtle.exportKey('raw', aesKey);
        const encryptedKey = await window.crypto.subtle.encrypt(
            { name: 'RSA-OAEP' },
            publicKey,
            exportedAesKey
        );

        // 5. JWE Compact Serialization 조립
        const encKey = base64UrlEncode(encryptedKey);
        const ivStr = base64UrlEncode(iv);
        const cipherStr = base64UrlEncode(ciphertext);
        const tagStr = base64UrlEncode(authTag);

        return {
            reqPayload: `${header}.${encKey}.${ivStr}.${cipherStr}.${tagStr}`,
            aesKey
        };
    } catch (error) {
        console.error('하이브리드 암호화 실패:', error);
        return null;
    }
};

/**
 * 은행으로부터 받은 암호화된 응답을 복호화
 * @param resPayload IV + CipherText + Tag (Base64 인코딩)
 * @param aesKey 요청 시 사용했던 AES 키
 */
export const decryptBankResponse = async (
    resPayload: string,
    aesKey: CryptoKey
): Promise<any> => {
    try {
        const encryptedBytes = base64UrlDecode(resPayload);
        const iv = encryptedBytes.slice(0, 12);
        const data = encryptedBytes.slice(12);

        const decryptedBuffer = await window.crypto.subtle.decrypt(
            { name: 'AES-GCM', iv },
            aesKey,
            data
        );

        return JSON.parse(new TextDecoder().decode(decryptedBuffer));
    } catch (error) {
        console.error('응답 복호화 실패:', error);
        throw new Error('응답 데이터를 해독할 수 없습니다.');
    }
};

/**
 * 보안 요청 준비 (암호화 + 서명 통합) - aesKey를 함께 반환하여 호출자가 보관하도록 함
 */
export const prepareSecureRequest = async (
    sensitiveData: object,
    nonSensitiveData: object,
    bankCode: string
) => {
    try {
        const keyId = useBankKeyStore.getState().getBankKeyId(bankCode);
        if (!keyId) throw new Error(`은행[${bankCode}]의 키 ID가 없습니다.`);

        const encryptionResult = await hybridEncrypt(sensitiveData, bankCode);
        if (!encryptionResult) throw new Error('암호화 실패');

        const { reqPayload, aesKey } = encryptionResult;

        const jwsSignature = await createJwsSignature({
            reqPayload,
            ...nonSensitiveData,
            timestamp: Date.now()
        });

        return {
            payload: { reqPayload, ...nonSensitiveData },
            headers: { 'x-jws-signature': jwsSignature, 'x-bank-key-id': keyId },
            aesKey // 응답 복호화를 위해 반환
        };
    } catch (error) {
        console.error('보안 요청 준비 실패:', error);
        return null;
    }
};

/**
 * 이체 보안 요청 준비 (멀티 타겟 암호화 + 통합 서명)
 */
export const prepareTransferSecureRequest = async (
    withdrawData: object,
    depositData: object,
    transferInfo: object
) => {
    try {
        const withdrawBankCode = (transferInfo as any).withdrawalBankCode;
        const depositBankCode = (transferInfo as any).depositBankCode;

        const withdrawKeyId = useBankKeyStore.getState().getBankKeyId(withdrawBankCode);
        if (!withdrawKeyId) throw new Error(`출금은행[${withdrawBankCode}]의 키 ID가 없습니다.`);

        const depositKeyId = useBankKeyStore.getState().getBankKeyId(depositBankCode);
        if (!depositKeyId) throw new Error(`입금은행[${depositBankCode}]의 키 ID가 없습니다.`);

        // 1. 출금 정보 암호화
        const withdrawResult = await hybridEncrypt(withdrawData, withdrawBankCode);
        if (!withdrawResult) throw new Error('출금 정보 암호화 실패');

        // 2. 입금 정보 암호화
        const depositResult = await hybridEncrypt(depositData, depositBankCode);
        if (!depositResult) throw new Error('입금 정보 암호화 실패');

        // 3. 통합 페이로드 구성
        const combinedPayload = {
            withdrawReqPayload: withdrawResult.reqPayload,
            depositReqPayload: depositResult.reqPayload,
            ...transferInfo
        };

        // 4. 통합 JWS 서명
        const jwsSignature = await createJwsSignature({
            ...combinedPayload,
            timestamp: Date.now()
        });

        return {
            payload: combinedPayload,
            headers: { 
                'x-jws-signature': jwsSignature, 
                'x-withdraw-key-id': withdrawKeyId,
                'x-deposit-key-id': depositKeyId 
            },
            withdrawAesKey: withdrawResult.aesKey,
            depositAesKey: depositResult.aesKey
        };
    } catch (error) {
        console.error('이체 보안 요청 준비 실패:', error);
        return null;
    }
};
