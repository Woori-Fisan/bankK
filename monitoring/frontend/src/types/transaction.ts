export interface TransactionSummaryRequest {
    startDate: string;
    endDate: string;
    agencyCode?: string;
    bankCode?: string;
}

export interface TransactionSummaryResponse {
    totalCount: number;
    successCount: number;
    errorCount: number;
    successRate: number;
    avgElapsedMs: number;
}
