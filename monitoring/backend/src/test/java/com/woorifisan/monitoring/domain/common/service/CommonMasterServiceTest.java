package com.woorifisan.monitoring.domain.common.service;

import com.woorifisan.monitoring.domain.common.dto.response.AgencyResponseDTO;
import com.woorifisan.monitoring.domain.common.dto.response.BankResponseDTO;
import com.woorifisan.monitoring.domain.common.mapper.CommonMasterMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommonMasterServiceTest {

    @InjectMocks
    private CommonMasterService commonMasterService;

    @Mock
    private CommonMasterMapper commonMasterMapper;

    @Test
    @DisplayName("활성화된_은행_목록을_조회한다")
    void getBankList_returnsActiveBanks() {
        // given
        List<BankResponseDTO> expectedBanks = new ArrayList<>();
        expectedBanks.add(new BankResponseDTO());
        given(commonMasterMapper.findAllActiveBanks()).willReturn(expectedBanks);

        // when
        List<BankResponseDTO> result = commonMasterService.getBankList();

        // then
        assertThat(result).isNotNull().hasSize(1);
        verify(commonMasterMapper, times(1)).findAllActiveBanks();
    }

    @Test
    @DisplayName("활성화된_대행기관_목록을_조회한다")
    void getAgencyList_returnsActiveAgencies() {
        // given
        List<AgencyResponseDTO> expectedAgencies = new ArrayList<>();
        expectedAgencies.add(new AgencyResponseDTO());
        given(commonMasterMapper.findAllActiveAgencies()).willReturn(expectedAgencies);

        // when
        List<AgencyResponseDTO> result = commonMasterService.getAgencyList();

        // then
        assertThat(result).isNotNull().hasSize(1);
        verify(commonMasterMapper, times(1)).findAllActiveAgencies();
    }
}
