package com.woorifisan.platform.domain.bank.service;

import com.woorifisan.platform.domain.bank.dto.response.BankDto;
import com.woorifisan.platform.domain.bank.mapper.BankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankMapper bankMapper;

    public List<BankDto> getActiveBanks() {
        return bankMapper.findAll().stream()
                .filter(bank -> bank.isActive())
                .map(bank -> BankDto.builder()
                        .bankCode(bank.getBankCode())
                        .bankName(bank.getBankName())
                        .build())
                .collect(Collectors.toList());
    }
}
