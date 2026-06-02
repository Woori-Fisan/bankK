package com.woorifisan.platform.domain.bank.dto.response;

import lombok.*;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 이체 실행 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    private String transactionId;
    private String transactionDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal balanceAfter;

}
