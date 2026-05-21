package com.woorifisan.platform.global.util;

import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class CryptoUtil {

    /**
     * JWE(비밀번호) 복호화
     */
    public static String decryptJwe(String jweString, String privateKeyPem) {
        try {
            RSAPrivateKey privateKey = parsePrivateKey(privateKeyPem);
            JWEObject jweObject = JWEObject.parse(jweString);
            jweObject.decrypt(new RSADecrypter(privateKey));
            return jweObject.getPayload().toString();
        } catch (Exception e) {
            log.error("JWE 복호화 실패", e);
            throw new RuntimeException("비밀번호 복호화에 실패했습니다.", e);
        }
    }

    /**
     * JWS 서명 검증 및 페이로드 반환
     * @return 검증 성공 시 페이로드 문자열 반환, 실패 시 null 반환
     */
    public static String verifyJwsAndGetPayload(String jwsString, String publicKeyPem) {
        try {
            RSAPublicKey publicKey = parsePublicKey(publicKeyPem);
            JWSObject jwsObject = JWSObject.parse(jwsString);
            
            if (jwsObject.verify(new RSASSAVerifier(publicKey))) {
                return jwsObject.getPayload().toString();
            }
            return null;
        } catch (Exception e) {
            log.error("JWS 서명 검증 에러", e);
            return null;
        }
    }

    /**
     * PEM 형식의 개인키 문자열을 RSAPrivateKey 객체로 변환
     */
    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
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

    /**
     * PEM 형식의 공개키 문자열을 RSAPublicKey 객체로 변환
     */
    private static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String publicKeyPEM = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
}
