import { create } from 'zustand';

interface AuthState {
    userId: string | null;
    userRole: string | null;
    accessToken: string | null;
    loginTime: string | null;
    tokenExpiry: number | null;
    setUserId: (id: string | null) => void;
    setUserRole: (role: string | null) => void;
    setAccessToken: (token: string | null) => void;
    setLoginTime: (time: string | null) => void;
    setTokenExpiry: (expiry: number | null) => void;
    isAdmin: () => boolean;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
    userId: null,
    userRole: null,
    accessToken: null,
    loginTime: null,
    tokenExpiry: null,
    setUserId: (id) => set({ userId: id }),
    setUserRole: (role) => set({ userRole: role }),
    setAccessToken: (token) => set({ accessToken: token }),
    setLoginTime: (time) => set({ loginTime: time }),
    setTokenExpiry: (expiry) => set({ tokenExpiry: expiry }),
    isAdmin: () => get().userRole === 'AGENCY_ADMIN',
    clearAuth: () => set({ userId: null, userRole: null, accessToken: null, loginTime: null, tokenExpiry: null }),
}));
