package com.woorifisan.monitoring.domain.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "은행 마스터 정보 응답")
public class BankResponseDTO {
    @Schema(description = "은행 코드", example = "020")
    private String bankCode;

    @Schema(description = "은행 명칭", example = "우리은행")
    private String bankName;
}
