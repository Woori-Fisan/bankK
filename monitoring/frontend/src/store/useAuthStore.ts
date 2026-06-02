import { create } from 'zustand';

interface AuthState {
    userId: string | null;
    accessToken: string | null;
    loginTime: string | null;
    tokenExpiry: number | null;
    setUserId: (id: string | null) => void;
    setAccessToken: (token: string | null) => void;
    setLoginTime: (time: string | null) => void;
    setTokenExpiry: (expiry: number | null) => void;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    userId: null,
    accessToken: null,
    loginTime: null,
    tokenExpiry: null,
    setUserId: (id) => set({ userId: id }),
    setAccessToken: (token) => set({ accessToken: token }),
    setLoginTime: (time) => set({ loginTime: time }),
    setTokenExpiry: (expiry) => set({ tokenExpiry: expiry }),
    clearAuth: () => set({ userId: null, accessToken: null, loginTime: null, tokenExpiry: null }),
}));
