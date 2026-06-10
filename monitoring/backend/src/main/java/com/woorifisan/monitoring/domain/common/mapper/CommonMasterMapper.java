package com.woorifisan.monitoring.domain.common.mapper;

import com.woorifisan.monitoring.domain.common.dto.response.AgencyResponseDTO;
import com.woorifisan.monitoring.domain.common.dto.response.BankResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommonMasterMapper {
    /**
     * 활성화된 모든 은행 목록을 조회합니다.
     */
    List<BankResponseDTO> findAllActiveBanks();

    /**
     * 활성화된 모든 대행기관 목록을 조회합니다.
     */
    List<AgencyResponseDTO> findAllActiveAgencies();
}
