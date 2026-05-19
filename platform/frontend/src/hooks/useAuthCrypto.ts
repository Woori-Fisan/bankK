import { useState, useCallback } from 'react';
import { encryptPassword, createJwsSignature } from '../utils/authCrypto';

interface AuthCryptoResult {
    encryptAndSign: (password: string, payload: object, platformPublicKey: string) => Promise<{ encryptedPassword: string | null; jwsSignature: string | null } | null>;
    isLoading: boolean;
    error: string | null;
}

export const useAuthCrypto = (): AuthCryptoResult => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // TODO: 실제 Private Key는 안전하게 관리되어야 합니다.
    // JWS 서명을 위한 임시 Private Key를 사용합니다.
    const DUMMY_CLIENT_PRIVATE_KEY = `-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCp4h4j4j4j4j4j
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
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4jj4j4j4j4j4
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
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j3D
-----END PRIVATE KEY-----`;
    
    const encryptAndSign = useCallback(async (password: string, payload: object, platformPublicKey: string) => {
        setIsLoading(true);
        setError(null);
        try {
            const encryptedPassword = await encryptPassword(password, platformPublicKey);
            const jwsSignature = await createJwsSignature(payload, DUMMY_CLIENT_PRIVATE_KEY);

            if (encryptedPassword && jwsSignature) {
                return { encryptedPassword, jwsSignature };
            } else {
                setError("Encryption or signature generation failed.");
                return null;
            }
        } catch (e: any) {
            setError(e.message);
            return null;
        } finally {
            setIsLoading(false);
        }
    }, [DUMMY_CLIENT_PRIVATE_KEY]);

    return { encryptAndSign, isLoading, error };
};
