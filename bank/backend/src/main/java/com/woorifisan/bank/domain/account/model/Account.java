package com.woorifisan.bank.domain.account.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Account {
    /** PK */
    private Long id;
    
    /** 고객 ID */
    private Long customerId;
    
    /** 계좌번호 (평문) - 마이그레이션 후 제거 또는 내부용으로만 사용 */
    private String accountNo;
    
    /** 암호화된 계좌번호 (AES-256-GCM) */
    private String accountNoEnc;
    
    /** 계좌번호 해시값 (SHA-256 + 고정 Salt) - 블라인드 인덱스용 */
    private String accountNoHash;
    
    /** 계좌 비밀번호 (bcrypt) */
    private String password;
    
    /** 계좌 유형 (예: DEPOSIT) */
    private String accountType;
    
    /** 잔액 */
    private BigDecimal balance;
    
    /** 계좌 상태 (예: NORMAL, LOCKED) */
    private String status;
    
    /** 생성일시 */
    private LocalDateTime createdAt;
    
    /** 수정일시 */
    private LocalDateTime updatedAt;
}
