// platform/frontend/src/store/useEmployeeStore.ts
import { create } from 'zustand';
import type { Employee, EmployeePagination } from '../types/employee';

interface EmployeeState {
    employees: Employee[];
    totalCount: number;
    totalPages: number;
    currentPage: number;
    selectedEmployee: Employee | null;
    isLoading: boolean;
    error: string | null;
    filters: {
        page: number;
        size: number;
        agencyId?: number;
    };
}

interface EmployeeActions {
    setEmployees: (pagination: EmployeePagination) => void;
    setSelectedEmployee: (employee: Employee | null) => void;
    setLoading: (loading: boolean) => void;
    setError: (error: string | null) => void;
    setPage: (page: number) => void;
    setFilterAgencyId: (agencyId?: number) => void;
    resetState: () => void;
}

const initialState: EmployeeState = {
    employees: [],
    totalCount: 0,
    totalPages: 0,
    currentPage: 0,
    selectedEmployee: null,
    isLoading: false,
    error: null,
    filters: {
        page: 0,
        size: 20,
    },
};

export const useEmployeeStore = create<EmployeeState & EmployeeActions>((set) => ({
    ...initialState,
    setEmployees: (pagination) => set({
        employees: pagination.employees,
        totalCount: pagination.totalCount,
        totalPages: pagination.totalPages,
        currentPage: pagination.currentPage,
    }),
    setSelectedEmployee: (employee) => set({ selectedEmployee: employee }),
    setLoading: (loading) => set({ isLoading: loading }),
    setError: (error) => set({ error: error }),
    setPage: (page) => set((state) => ({ filters: { ...state.filters, page } })),
    setFilterAgencyId: (agencyId) => set((state) => ({ filters: { ...state.filters, agencyId } })),
    resetState: () => set(initialState),
}));
