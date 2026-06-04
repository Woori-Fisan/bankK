package com.woorifisan.platform.domain.bank.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HistoryInquiryResponse extends SecureResponse {

    private Integer totalCount;
    private Integer totalPages;
    private Integer currentPage;
    private Integer size;
    private Boolean hasNext;

}
