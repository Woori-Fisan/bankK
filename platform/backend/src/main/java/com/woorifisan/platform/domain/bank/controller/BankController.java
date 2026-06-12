package com.woorifisan.platform.domain.bank.controller;

import com.woorifisan.platform.domain.bank.dto.response.BankDto;
import com.woorifisan.platform.domain.bank.service.BankService;
import com.woorifisan.platform.global.aop.annotation.ExcludeLogging;
import com.woorifisan.platform.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExcludeLogging
@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping
    public ApiResponse<List<BankDto>> getBanks() {
        return ApiResponse.success(bankService.getActiveBanks());
    }
}
