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
}

export type LoginApiResponse = ApiCommonResponse<LoginResponse>;
