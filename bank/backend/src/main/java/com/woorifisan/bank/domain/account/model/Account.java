package com.woorifisan.bank.domain.account.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계좌 도메인 모델 (원장 정보)
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Account {

    private Long id;                // 고유 ID
    private Long customerId;        // 고객 고유 ID (FK)
    private String accountNoEnc;    // 계좌번호 (AES-256 암호화)
    private String accountNoHash;   // 계좌번호 검색용 해시 (SHA-256, Blind Index)
    private String passwordHash;    // 계좌 비밀번호 해시 (bcrypt)
    private String accountType;     // 계좌 유형 (예금, 적금, 대출 등)
    private BigDecimal balance;     // 현재 잔액
    private String status;          // 계좌 상태 (NORMAL, LOCKED, CLOSED)
    private Integer version;        // 낙관적 락 제어용 버전
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
}
