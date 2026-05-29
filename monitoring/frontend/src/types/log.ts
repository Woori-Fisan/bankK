import type { ApiCommonResponse } from './common';

export interface SystemLog {
    id: number;
    createdAt: string;
    level: 'INFO' | 'WARN' | 'ERROR';
    logType: string;
    traceId: string;
    staffId: string;
    bankCode: string;
    targetCode: string;
    agencyCode: string;
    bankKeyId: string;
    httpMethod: string;
    httpUri: string;
    httpStatus: number;
    elapsedMs: number;
    clientIp: string;
    jwsSignature: string;
    bodyData: string;
    errorCode: string | null;
    errorMessage: string | null;
}

export interface LogListRequest {
    startDate: string;
    endDate: string;
    agencyCode?: string;
    bankCode?: string;
    staffId?: string;
    logType?: string;
    httpStatus?: string;
    page?: number;
    size?: number;
}

export interface LogListDTO {
    pageNum: number;
    pageSize: number;
    totalPage: number;
    logListDTO: SystemLog[];
}

export type LogListApiResponse = ApiCommonResponse<LogListDTO>;

export type LogApiResponse = ApiCommonResponse<SystemLog>;