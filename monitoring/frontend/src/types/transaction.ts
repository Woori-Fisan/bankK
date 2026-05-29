import type { ApiCommonResponse } from './common';

export interface TransactionSummaryRequest {
    startDate: string;
    endDate: string;
    agencyCode?: string;
    bankCode?: string;
    staffId?: string;
    logType?: string;
    httpStatus?: string;
}

export interface TransactionSummaryResponse {
    totalCount: number;
    successCount: number;
    errorCount: number;
    successRate: number;
    averageElapsedMs: number;
}


export type TransactionSummaryApiResponse = ApiCommonResponse<TransactionSummaryResponse>