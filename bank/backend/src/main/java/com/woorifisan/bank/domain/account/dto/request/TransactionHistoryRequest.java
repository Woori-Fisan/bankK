package com.woorifisan.bank.domain.account.dto.request;

import com.woorifisan.bank.global.security.dto.SecureRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "거래 내역 조회 요청 객체")
public class TransactionHistoryRequest extends SecureRequest {

    @Pattern(
        regexp = "^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$",
        message = "날짜 형식은 yyyy-MM-dd 이어야 합니다."
    )
    @Schema(description = "조회 시작일", example = "2024-01-01")
    @NotBlank(message = "조회 시작일은 필수입니다.")
    private String startDate;

    @Pattern(
        regexp = "^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$",
        message = "날짜 형식은 yyyy-MM-dd 이어야 합니다."
    )
    @Schema(description = "조회 종료일", example = "2024-01-31")
    @NotBlank(message = "조회 종료일은 필수입니다.")
    private String endDate;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    @lombok.Builder.Default
    private Integer page = 0;

    @Schema(description = "페이지당 건수 (최대 100)", example = "20")
    @Min(value = 1, message = "페이지 당 건수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 당 건수는 최대 100건입니다.")
    @lombok.Builder.Default
    private Integer size = 20;
}
