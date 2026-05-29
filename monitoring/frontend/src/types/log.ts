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
