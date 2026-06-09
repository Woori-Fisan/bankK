package com.woorifisan.bank.domain.customer.service;

import com.woorifisan.bank.domain.customer.mapper.CustomerMapper;
import com.woorifisan.bank.domain.customer.model.Customer;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;

    /**
     * 고객 본인 확인
     */
    public void verifyCustomerIdentification(Long customerId, String requestRrnPrefix, String customerName) {
        Customer customer = customerMapper.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 계좌번호로 조회한 사용자와 일치 여부 검증
        if (
                !customer.getRrnPrefix().equals(requestRrnPrefix)
                || !customer.getCustomerName().equals(customerName))
        {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
    }
}
