package com.woorifisan.platform.domain.bank.external.dto;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 은행 코어 시스템으로 전송할 거래 내역 조회 요청 DTO
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankHistoryInquiryRequest extends SecureRequest {

    private String startDate;
    private String endDate;
    private Integer page;
    private Integer size;

    public static BankHistoryInquiryRequest of(com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest request, String bankKeyId) {
        return BankHistoryInquiryRequest.builder()
                .reqPayload(request.getReqPayload())
                .bankKeyId(bankKeyId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }
}
