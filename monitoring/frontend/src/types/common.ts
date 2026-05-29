export interface ApiCommonResponse<T> {
    success: boolean;
    data?: T;
    error?: ErrorResponse;
}

export interface ErrorResponse {
    code: string;
    message: string;
}
