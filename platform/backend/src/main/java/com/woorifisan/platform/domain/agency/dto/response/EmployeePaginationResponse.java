package com.woorifisan.platform.domain.agency.dto.response;

import com.woorifisan.platform.domain.agency.dto.EmployeeDto;

import java.util.List;

public record EmployeePaginationResponse(
        int totalCount,
        int totalPages,
        int currentPage,
        List<EmployeeDto> employees
) {
}