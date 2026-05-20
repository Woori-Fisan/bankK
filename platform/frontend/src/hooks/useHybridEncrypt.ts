import { useCallback } from 'react';
import { importSPKI, CompactEncrypt } from 'jose';
import { useBankKeyStore } from '../store/useBankKeyStore';

const DEV_FALLBACK_KEY = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyYt9L2K6pD8h5t2F2c6M
9zX7P5tD7c4M4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j4j
AQAB
-----END PUBLIC KEY-----`;

export const useHybridEncrypt = () => {
  const getBankPublicKey = useBankKeyStore((s) => s.getBankPublicKey);

  const encrypt = useCallback(
    async (plaintext: string, bankCode: string): Promise<string> => {
      const pem = getBankPublicKey(bankCode) ?? DEV_FALLBACK_KEY;
      const publicKey = await importSPKI(pem, 'RSA-OAEP-256');
      const bytes = new TextEncoder().encode(plaintext);
      return new CompactEncrypt(bytes)
        .setProtectedHeader({ alg: 'RSA-OAEP-256', enc: 'A256GCM' })
        .encrypt(publicKey);
    },
    [getBankPublicKey],
  );

  return { encrypt };
};
