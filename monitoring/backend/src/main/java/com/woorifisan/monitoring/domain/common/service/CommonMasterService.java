package com.woorifisan.monitoring.domain.common.service;

import com.woorifisan.monitoring.domain.common.dto.response.AgencyResponseDTO;
import com.woorifisan.monitoring.domain.common.dto.response.BankResponseDTO;
import com.woorifisan.monitoring.domain.common.mapper.CommonMasterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonMasterService {
    private final CommonMasterMapper commonMasterMapper;

    public List<BankResponseDTO> getBankList() {
        log.info("[Service] 은행 목록 조회 요청");
        return commonMasterMapper.findAllActiveBanks();
    }

    public List<AgencyResponseDTO> getAgencyList() {
        log.info("[Service] 대행기관 목록 조회 요청");
        return commonMasterMapper.findAllActiveAgencies();
    }
}
