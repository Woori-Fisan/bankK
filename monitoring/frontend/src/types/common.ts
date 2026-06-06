export interface ApiCommonResponse<T> {
    success: boolean;
    data?: T;
    error?: ErrorResponse;
}

export interface ErrorResponse {
    code: string;
    message: string;
}

export interface Agency {
    agencyCode: string;
    agencyName: string;
}

export type AgencyListApiResponse = ApiCommonResponse<Agency[]>;

export interface Bank {
    bankCode: string;
    bankName: string;
}

export type BankListApiResponse = ApiCommonResponse<Bank[]>;
