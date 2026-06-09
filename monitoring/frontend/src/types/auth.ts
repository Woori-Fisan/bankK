import type { ApiCommonResponse } from './common';

export interface RegisterRequest {
    loginId: string;
    password: string;
    name: string;
}

export interface LoginRequest {
    loginId: string;
    password: string;
}

export interface LoginResponse {
    accessToken: string;
    loginId: string;
    refreshTokenExpiresIn?: number;
}

export interface TokenRefreshResponse {
    accessToken: string;
    refreshTokenExpiresIn: number;
}

export type LoginApiResponse = ApiCommonResponse<LoginResponse>;
