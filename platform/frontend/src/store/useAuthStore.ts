import { create } from 'zustand';

interface AuthState {
    userRole: string | null;
    setUserRole: (role: string | null) => void;
    isAdmin: () => boolean;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
    userRole: null,
    setUserRole: (role) => set({ userRole: role }),
    isAdmin: () => get().userRole === 'AGENCY_ADMIN',
    clearAuth: () => set({ userRole: null }),
}));
