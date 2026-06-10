package com.woorifisan.platform.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 단말기 JWS 전자서명 검증을 활성화하는 애노테이션입니다.
 * 컨트롤러 메소드에 적용하며, x-jws-signature 헤더를 검증합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyTerminalSignature {
}
