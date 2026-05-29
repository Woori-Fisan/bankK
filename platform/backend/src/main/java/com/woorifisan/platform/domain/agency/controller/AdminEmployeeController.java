package com.woorifisan.platform.domain.agency.controller;

import com.woorifisan.platform.global.response.ApiResponse;
import com.woorifisan.platform.domain.agency.dto.response.EmployeeDeleteResponse;
import com.woorifisan.platform.domain.agency.dto.response.EmployeePaginationResponse;
import com.woorifisan.platform.domain.agency.dto.response.EmployeePasswordResetResponse;
import com.woorifisan.platform.domain.agency.dto.request.EmployeePasswordResetRequest;
import com.woorifisan.platform.domain.agency.dto.request.EmployeeRegisterRequest;
import com.woorifisan.platform.domain.agency.service.AdminEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    /**
     * 4.3.1. 직원 목록 조회
     */
    @GetMapping
    public ApiResponse<EmployeePaginationResponse> getEmployeeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agencyId) {
        
        EmployeePaginationResponse response = adminEmployeeService.getEmployeeList(page, size, agencyId);
        return ApiResponse.success(response);
    }

    /**
     * 4.3.2. 직원 등록
     */
    @PostMapping
    public ApiResponse<Void> registerEmployee(@Valid @RequestBody EmployeeRegisterRequest request) {
        adminEmployeeService.registerEmployee(request);
        return ApiResponse.success();
    }

    /**
     * 4.3.3. 직원 삭제
     */
    @DeleteMapping("/{loginId}")
    public ApiResponse<EmployeeDeleteResponse> deleteEmployee(@PathVariable String loginId) {
        EmployeeDeleteResponse response = adminEmployeeService.deleteEmployee(loginId);
        return ApiResponse.success(response);
    }

    /**
     * 4.3.4. 비밀번호 초기화
     */
    @PatchMapping("/{loginId}/password/reset")
    public ApiResponse<EmployeePasswordResetResponse> resetPassword(
            @PathVariable String loginId,
            @Valid @RequestBody EmployeePasswordResetRequest request) {
        
        EmployeePasswordResetResponse response = adminEmployeeService.resetPassword(loginId, request);
        return ApiResponse.success(response);
    }
}
