package com.woorifisan.bank.domain.customer.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Customer {

    private Long id;                // 고유 ID
    private String ciHash;          // 고객 식별 해시 (SHA-256)
    private String customerName;    // 고객명
    private String rrnPrefixEnc;    // 주민번호 앞 7자리 (AES-256 암호화)
    private Integer genderCode;     // 성별 코드
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시

}
