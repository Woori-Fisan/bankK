package com.woorifisan.bank.domain.account.dto.response;

import com.woorifisan.bank.global.security.dto.SecureResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "거래 내역 조회 응답 객체")
public class TransactionHistoryResponse extends SecureResponse {

    @Schema(description = "전체 거래 건수", example = "100")
    private Integer totalCount;

    @Schema(description = "전체 페이지 수", example = "5")
    private Integer totalPages;

    @Schema(description = "현재 페이지 번호", example = "0")
    private Integer currentPage;

    @Schema(description = "페이지당 건수", example = "20")
    private Integer size;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private Boolean hasNext;

    /**
     * 암호화될 민감 데이터 구조
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SensitiveData {
        private List<TransactionHistoryDto> history;
    }

    public static TransactionHistoryResponse of(
            String resPayload, Integer totalCount, Integer totalPages, 
            Integer currentPage, Integer size, Boolean hasNext) {
        return TransactionHistoryResponse.builder()
                .resPayload(resPayload)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .size(size)
                .hasNext(hasNext)
                .build();
    }
}
