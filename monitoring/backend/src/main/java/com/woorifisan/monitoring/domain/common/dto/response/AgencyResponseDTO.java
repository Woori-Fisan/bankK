package com.woorifisan.monitoring.domain.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "대행기관 마스터 정보 응답")
public class AgencyResponseDTO {
    @Schema(description = "대행기관 코드", example = "PO001")
    private String agencyCode;

    @Schema(description = "대행기관 명칭", example = "우체국")
    private String agencyName;
}
