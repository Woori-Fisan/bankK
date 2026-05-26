package com.woorifisan.platform.domain.bank.external.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코어 시스템으로 전송할 거래 내역 조회 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BankHistoryInquiryRequest {

    private String encryptedKey;
    private String jwsSignature;
    private String accountNo;
    private String customerRrnPrefix;
    private String startDate;
    private String endDate;
    private Integer page;
    private Integer size;

    public static BankHistoryInquiryRequest from(com.woorifisan.platform.domain.bank.dto.request.HistoryInquiryRequest request) {
        return BankHistoryInquiryRequest.builder()
                .encryptedKey(request.getEncryptedKey())
                .jwsSignature(request.getJwsSignature())
                .accountNo(request.getAccountNo())
                .customerRrnPrefix(request.getCustomerRrnPrefix())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .page(request.getPage())
                .size(request.getSize())
                .build();
    }
}
