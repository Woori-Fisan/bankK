import type { AgencyListApiResponse, BankListApiResponse } from '../types/common';
import { axiosTokenInstance } from './axiosInstance';

export const getAgencies = async (): Promise<AgencyListApiResponse> => {
    const response = await axiosTokenInstance.get<AgencyListApiResponse>('/monitor/common/agencies');
    return response.data;
};

export const getBanks = async (): Promise<BankListApiResponse> => {
    const response = await axiosTokenInstance.get<BankListApiResponse>('/monitor/common/banks');
    return response.data;
};
