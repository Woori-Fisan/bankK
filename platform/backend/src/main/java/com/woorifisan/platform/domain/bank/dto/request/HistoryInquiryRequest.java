package com.woorifisan.platform.domain.bank.dto.request;

import com.woorifisan.platform.global.security.dto.SecureRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HistoryInquiryRequest extends SecureRequest {

    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;

    @NotBlank(message = "조회 시작일은 필수입니다.")
    private String startDate;

    @NotBlank(message = "조회 종료일은 필수입니다.")
    private String endDate;

    @NotNull(message = "페이지 번호는 필수입니다.")
    private Integer page;

    @NotNull(message = "페이지당 건수는 필수입니다.")
    private Integer size;
}
