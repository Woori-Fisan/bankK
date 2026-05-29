import type { LogListApiResponse, LogListRequest } from '../types/log';
import { axiosTokenInstance } from './axiosInstance';

export const getLogList = async (request: LogListRequest): Promise<LogListApiResponse> => {
    const response = await axiosTokenInstance.get<LogListApiResponse>('/monitor/transactions', { params: request });
    return response.data;
};