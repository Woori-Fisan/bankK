package com.woorifisan.platform.domain.bank.dto.response;

import com.woorifisan.platform.global.security.dto.SecureResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BalanceInquiryResponse extends SecureResponse {

    private String status;

}
