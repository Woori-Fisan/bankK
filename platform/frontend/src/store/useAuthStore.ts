import { create } from 'zustand';

interface AuthState {
    userRole: string | null;
    accessToken: string | null;
    setUserRole: (role: string | null) => void;
    setAccessToken: (token: string | null) => void;
    isAdmin: () => boolean;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
    userRole: null,
    accessToken: null,
    setUserRole: (role) => set({ userRole: role }),
    setAccessToken: (token) => set({ accessToken: token }),
    isAdmin: () => get().userRole === 'AGENCY_ADMIN',
    clearAuth: () => set({ userRole: null, accessToken: null }),
}));
