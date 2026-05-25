package com.woorifisan.bank.domain.customer.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Customer {

    private Long id;
    private String ci;
    private String customerName;
    private String rrnPrefix;
    private Integer genderCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
