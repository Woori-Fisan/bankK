import type { LogListApiResponse, LogListRequest, LogApiResponse } from '../types/log';
import type { TransactionSummaryApiResponse, TransactionSummaryRequest } from '../types/transaction';
import { axiosTokenInstance } from './axiosInstance';

export const getLogList = async (request: LogListRequest): Promise<LogListApiResponse> => {
    const response = await axiosTokenInstance.get<LogListApiResponse>('/monitor/transactions', { params: request });
    return response.data;
};

export const getLog = async (id: number): Promise<LogApiResponse> => {
    const response = await axiosTokenInstance.get<LogApiResponse>(`/monitor/transactions/${id}`);
    return response.data;
};

export const getTransactionSummary = async (request: TransactionSummaryRequest): Promise<TransactionSummaryApiResponse> => {
    const response = await axiosTokenInstance.get<TransactionSummaryApiResponse>('/monitor/summary', { params: request });
    return response.data;
};