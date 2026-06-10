import type { RegisterEmployeeRequest, EmployeePagination, DeleteEmployeeResponse, ResetPasswordResponse } from '../types/employee';
import type { ApiResponse } from '../types/common';
import axiosInstance from './axiosInstance';
import { encryptPassword } from '../utils/authCrypto';

// 4.3.1. 직원 목록 조회
export const fetchEmployees = async (page: number, size: number, agencyId?: number): Promise<ApiResponse<EmployeePagination>> => {
    const response = await axiosInstance.get<ApiResponse<EmployeePagination>>('/admin/employees', {
        params: {
            page,
            size,
            ...(agencyId && { agencyId }), // agencyId가 존재할 때만 파라미터에 포함
        },
    });
    return response.data;
};

// 4.3.2. 직원 등록
export const registerEmployee = async (employeeData: RegisterEmployeeRequest): Promise<ApiResponse<null>> => {
    const encryptedPassword = await encryptPassword(employeeData.password);

    if (!encryptedPassword) {
        throw new Error('비밀번호 암호화에 실패했습니다.');
    }

    const payload = {
        ...employeeData,
        password: encryptedPassword, // RSA로 암호화된 비밀번호로 덮어쓰기
    };

    const response = await axiosInstance.post<ApiResponse<null>>('/admin/employees', payload);
    return response.data;
};

// 4.3.3. 직원 삭제
export const deleteEmployee = async (loginId: string): Promise<ApiResponse<DeleteEmployeeResponse>> => {
    const response = await axiosInstance.delete<ApiResponse<DeleteEmployeeResponse>>(`/admin/employees/${loginId}`);
    return response.data;
};

// 4.3.4. 비밀번호 초기화
export const resetEmployeePassword = async (loginId: string, newPassword: string): Promise<ApiResponse<ResetPasswordResponse>> => {
    const encryptedPassword = await encryptPassword(newPassword);

    if (!encryptedPassword) {
        throw new Error('비밀번호 암호화에 실패했습니다.');
    }

    const response = await axiosInstance.patch<ApiResponse<ResetPasswordResponse>>(`/admin/employees/${loginId}/password/reset`, {
        password: encryptedPassword
    });
    return response.data;
};
