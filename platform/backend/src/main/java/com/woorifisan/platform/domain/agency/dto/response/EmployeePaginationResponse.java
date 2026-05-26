package com.woorifisan.platform.domain.agency.dto.response;

import com.woorifisan.platform.domain.agency.dto.EmployeeDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePaginationResponse {
    private int totalCount;
    private int totalPages;
    private int currentPage;
    private List<EmployeeDto> employees;
}
