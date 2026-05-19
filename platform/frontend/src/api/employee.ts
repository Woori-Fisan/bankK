import type { RegisterEmployeeRequest, EmployeePagination, DeleteEmployeeResponse, ResetPasswordResponse, Employee } from '../types/employee';
import type { ApiResponse } from '../types/common';
import { encryptPassword, fetchPlatformPublicKey } from '../utils/authCrypto';

const BASE_URL = 'https://your-platform-api-domain/api/v1'; // 실제 API 연동 시 사용할 Base URL

// 더미 데이터
const DUMMY_EMPLOYEES: Employee[] = [
    { employeeId: 'EMP-001', agencyId: 100, role: 'ADMIN', isLocked: false, isDeleted: false, createdAt: '2023-10-24T09:15:00Z' },
    { employeeId: 'EMP-002', agencyId: 101, role: 'USER', isLocked: false, isDeleted: false, createdAt: '2023-10-24T10:30:00Z' },
    { employeeId: 'EMP-003', agencyId: 100, role: 'USER', isLocked: true, isDeleted: false, createdAt: '2023-10-23T16:45:00Z' },
    { employeeId: 'EMP-004', agencyId: 102, role: 'ADMIN', isLocked: false, isDeleted: false, createdAt: '2023-10-23T14:20:00Z' },
    { employeeId: 'EMP-005', agencyId: 101, role: 'USER', isLocked: false, isDeleted: false, createdAt: '2023-10-22T11:00:00Z' },
    { employeeId: 'EMP-006', agencyId: 100, role: 'USER', isLocked: false, isDeleted: true, createdAt: '2023-10-21T09:00:00Z' },
];

export const fetchEmployees = async (page: number, size: number, agencyId?: number): Promise<ApiResponse<EmployeePagination>> => {
    // API 호출 지연 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 500));

    let filteredEmployees = DUMMY_EMPLOYEES;
    if (agencyId) {
        filteredEmployees = filteredEmployees.filter(emp => emp.agencyId === agencyId);
    }

    const start = page * size;
    const end = start + size;
    const paginatedEmployees = filteredEmployees.slice(start, end);

    const totalCount = filteredEmployees.length;
    const totalPages = Math.ceil(totalCount / size);

    return {
        status: 'SUCCESS',
        code: 2000,
        message: '직원 목록을 성공적으로 불러왔습니다.',
        data: {
            employees: paginatedEmployees,
            totalCount,
            totalPages,
            currentPage: page,
        },
    };
};

export const registerEmployee = async (employeeData: RegisterEmployeeRequest): Promise<ApiResponse<null>> => {
    await new Promise(resolve => setTimeout(resolve, 500));

    const publicKey = await fetchPlatformPublicKey();
    const encryptedPassword = await encryptPassword(employeeData.password, publicKey);

    if (!encryptedPassword) {
        throw new Error('비밀번호 암호화에 실패했습니다.');
    }

    // 성공적인 등록 시뮬레이션
    const newEmployee: Employee = {
        employeeId: employeeData.loginId,
        agencyId: employeeData.agencyId,
        role: employeeData.role,
        isLocked: false,
        isDeleted: false,
        createdAt: new Date().toISOString(),
    };
    DUMMY_EMPLOYEES.push(newEmployee);

    return {
        status: 'SUCCESS',
        code: 2011,
        message: '직원을 성공적으로 등록했습니다.',
        data: null,
    };
};

export const deleteEmployee = async (loginId: string): Promise<ApiResponse<DeleteEmployeeResponse>> => {
    await new Promise(resolve => setTimeout(resolve, 500));

    const index = DUMMY_EMPLOYEES.findIndex(emp => emp.employeeId === loginId);
    if (index > -1) {
        DUMMY_EMPLOYEES[index].isDeleted = true; // 삭제 처리
        DUMMY_EMPLOYEES[index].isLocked = true;  // 삭제 시 잠금 처리

        return {
            status: 'SUCCESS',
            code: 2000,
            message: '직원을 성공적으로 삭제했습니다.',
            data: { 
                loginId, 
                isDeleted: true, 
                updatedAt: new Date().toISOString() 
            },
        };
    } else {
        throw new Error('직원을 찾을 수 없습니다.');
    }
};

export const resetEmployeePassword = async (loginId: string): Promise<ApiResponse<ResetPasswordResponse>> => {
    await new Promise(resolve => setTimeout(resolve, 500));

    const employee = DUMMY_EMPLOYEES.find(emp => emp.employeeId === loginId);
    if (employee) {
        const temporaryPassword = Math.random().toString(36).slice(-8); // 임시 비밀번호 생성
        const publicKey = await fetchPlatformPublicKey();
        const encryptedTempPassword = await encryptPassword(temporaryPassword, publicKey);

        if (!encryptedTempPassword) {
            throw new Error('임시 비밀번호 암호화에 실패했습니다.');
        }

        employee.isLocked = false; // 비밀번호 초기화 시 잠금 해제

        return {
            status: 'SUCCESS',
            code: 2000,
            message: '비밀번호를 성공적으로 초기화했습니다.',
            data: { 
                loginId, 
                temporaryPassword: encryptedTempPassword, 
                isLocked: false, 
                updatedAt: new Date().toISOString() 
            },
        };
    } else {
        throw new Error('직원을 찾을 수 없습니다.');
    }
};