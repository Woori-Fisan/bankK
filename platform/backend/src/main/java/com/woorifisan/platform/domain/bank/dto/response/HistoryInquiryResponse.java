package com.woorifisan.platform.domain.bank.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HistoryInquiryResponse {

    private Integer totalCount;
    private Integer totalPages;
    private Integer currentPage;
    private Integer size;
    private Boolean hasNext;
    private List<TransactionHistoryDto> history;

}
