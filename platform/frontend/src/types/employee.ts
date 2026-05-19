// platform/frontend/src/types/employee.ts
export interface Employee {
    employeeId: string;
    agencyId: number;
    role: 'USER' | 'ADMIN';
    isLocked: boolean;
    isDeleted: boolean;
    createdAt: string; // ISO-8601
}

export interface EmployeePagination {
    totalCount: number;
    totalPages: number;
    currentPage: number;
    employees: Employee[];
}

export interface RegisterEmployeeRequest {
    loginId: string;
    password: string; // RSA encrypted
    role: 'USER' | 'ADMIN';
    agencyId: number;
}

export interface DeleteEmployeeResponse {
    loginId: string;
    isDeleted: boolean;
    updatedAt: string; // ISO-8601
}

export interface ResetPasswordResponse {
    loginId: string;
    temporaryPassword: string; // RSA encrypted
    isLocked: boolean;
    updatedAt: string; // ISO-8601
}