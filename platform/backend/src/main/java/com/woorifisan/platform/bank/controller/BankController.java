package com.woorifisan.platform.bank.controller;

import com.woorifisan.platform.bank.dto.BankDto;
import com.woorifisan.platform.bank.service.BankService;
import com.woorifisan.platform.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
