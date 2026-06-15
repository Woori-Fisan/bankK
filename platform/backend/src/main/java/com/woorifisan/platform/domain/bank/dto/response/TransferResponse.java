package com.woorifisan.platform.domain.bank.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 이체 실행 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TransferResponse extends SecureResponse {

    private String transactionId;
    private String status;
    private String transactionDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal balanceAfter;

}
