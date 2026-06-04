package com.woorifisan.bank.domain.account.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 잔액 조회 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "잔액 조회 요청 정보")
public class BalanceInquiryRequest extends SecureRequest {
}
