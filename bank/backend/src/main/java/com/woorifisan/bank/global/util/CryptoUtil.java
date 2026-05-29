package com.woorifisan.bank.global.util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 보안 및 암호화 유틸리티
 * AES-256-GCM (DB 저장용) 및 SHA-256 (Blind Index용) 처리
 */
@Component
public class CryptoUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${DB_ENCRYPTION_KEY}")
    private String dbEncryptionKey;

    @Value("${BLIND_INDEX_SALT}")
    private String blindIndexSalt;

    /**
     * SHA-256 해시 생성 (Blind Index용)
     * @param plainText 평문
     * @return 해시값 (Base64 인코딩)
     */
    public String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedText = blindIndexSalt + plainText;
            byte[] hash = digest.digest(saltedText.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    /**
     * AES-256-GCM 암호화
     * @param plainText 평문
     * @return IV + CipherText + AuthTag (Base64 인코딩)
     */
    public String encrypt(String plainText) {
        try {
            byte[] keyBytes = decodeKey(dbEncryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("AES 암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AES-256-GCM 복호화
     * @param encryptedText IV + CipherText + AuthTag (Base64 인코딩)
     * @return 평문
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] keyBytes = decodeKey(dbEncryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new RuntimeException("AES 복호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 키 디코딩 (Plain String 또는 Base64 대응)
     */
    private byte[] decodeKey(String key) {
        try {
            return Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException e) {
            // Base64가 아니면 평문 바이트로 처리
            return key.getBytes();
        }
    }
}
