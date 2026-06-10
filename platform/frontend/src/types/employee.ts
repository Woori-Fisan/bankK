export interface Employee {
    loginId: string;
    employeeNum: string;
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
    employeeNum: string;
}

export interface DeleteEmployeeResponse {
    loginId: string;
    isDeleted: boolean;
    updatedAt: string; // ISO-8601
}

export interface ResetPasswordRequest {
    password: string; // RSA encrypted
}

export interface ResetPasswordResponse {
    loginId: string;
    isLocked: boolean;
    updatedAt: string; // ISO-8601
}