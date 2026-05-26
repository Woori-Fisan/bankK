package com.woorifisan.bank.domain.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "거래 내역 조회 응답 객체")
public class TransactionHistoryResponse {

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

    @Schema(description = "거래 내역 리스트")
    private List<TransactionHistoryDto> history;
}
