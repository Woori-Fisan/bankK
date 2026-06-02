package com.woorifisan.bank.global.util;

import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 보안 및 암호화 유틸리티
 * AES-256-GCM (DB 저장용) 및 SHA-256 (Blind Index용) 처리
 */
@Slf4j
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
     * JWE 복호화 결과 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class JweDecryptionResult {
        private final String payload;
        private final SecretKey cek;
    }

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
     * 전달받은 CEK를 사용하여 응답 데이터를 AES-256-GCM 암호화
     * @param plainText 응답 평문 (JSON)
     * @param cek 요청 복호화 시 추출된 대칭키
     * @return IV + CipherText + AuthTag (Base64 인코딩)
     */
    public String encryptWithCek(String plainText, SecretKey cek) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, cek, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("응답 데이터 암호화 실패", e);
            throw new RuntimeException("응답 암호화 중 오류가 발생했습니다.", e);
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
     * 전달받은 CEK를 사용하여 바이너리 데이터를 AES-256-GCM 복호화
     * @param encryptedBytes IV + CipherText + AuthTag 바이트 배열
     * @param cek 요청 복호화 시 추출된 대칭키
     * @return 복호화된 평문 바이트 배열
     */
    public byte[] decryptWithCek(byte[] encryptedBytes, SecretKey cek) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, cek, parameterSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            log.error("바이너리 데이터 복호화 실패", e);
            throw new RuntimeException("바이너리 복호화 중 오류가 발생했습니다.", e);
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

    /**
     * JWE(민감데이터) 복호화 (RSA-OAEP-256)
     * @param jweString 암호화된 JWE 문자열
     * @param privateKeyPem RSA 개인키 (PEM 포맷)
     * @return 복호화된 페이로드 (JSON 문자열)
     */
    public String decryptJwe(String jweString, String privateKeyPem) {
        return decryptJweWithKey(jweString, privateKeyPem).getPayload();
    }

    /**
     * JWE 복호화 및 CEK(대칭키) 추출
     * @param jweString 암호화된 JWE 문자열
     * @param privateKeyPem RSA 개인키 (PEM 포맷)
     * @return 페이로드와 CEK가 포함된 결과 객체
     */
    public JweDecryptionResult decryptJweWithKey(String jweString, String privateKeyPem) {
        try {
            RSAPrivateKey privateKey = parsePrivateKey(privateKeyPem);
            JWEObject jweObject = JWEObject.parse(jweString);

            // 1. RSA-OAEP-256을 사용하여 CEK(Content Encryption Key) 직접 복호화
            byte[] encryptedKey = jweObject.getEncryptedKey().decode();
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT
            );
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            byte[] cekBytes = rsaCipher.doFinal(encryptedKey);
            SecretKey cek = new SecretKeySpec(cekBytes, "AES");

            // 2. 추출된 CEK를 사용하여 JWE 객체 복호화 (Nimbus 라이브러리 활용)
            // 내부적으로 jweObject의 header(AAD), iv, ciphertext, tag를 사용함
            jweObject.decrypt(new RSADecrypter(privateKey));

            return new JweDecryptionResult(jweObject.getPayload().toString(), cek);
        } catch (Exception e) {
            log.error("JWE 복호화 실패 상세: {}", e.getMessage());
            // Tag mismatch 발생 시 원인 분석을 위한 추가 로그
            if (e.getMessage().contains("Tag mismatch")) {
                log.error("인증 태그 불일치 발생: 프론트엔드의 AAD(Header) 처리 또는 IV/Tag 인코딩을 확인해야 합니다.");
            }
            throw new RuntimeException("데이터 복호화에 실패했습니다. (보안 무결성 검증 실패)", e);
        }
    }

    /**
     * PEM 형식의 개인키 문자열을 RSAPrivateKey 객체로 변환
     */
    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
