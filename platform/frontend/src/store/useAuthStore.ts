import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface AuthState {
    userRole: string | null;
    setUserRole: (role: string | null) => void;
    isAdmin: () => boolean;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set, get) => ({
            userRole: null,
            setUserRole: (role) => set({ userRole: role }),
            isAdmin: () => get().userRole === 'AGENCY_ADMIN',
            clearAuth: () => set({ userRole: null }),
        }),
        {
            name: 'auth-storage',
            storage: createJSONStorage(() => localStorage),
        }
    )
);
