import axiosInstance from './axiosInstance';
import type { ApiResponse } from '../types/common';

export interface ReviewDocument {
  documentType: string;
  documentName: string;
  documentUrl: string;
  documentContent?: string;
  isMandatory: boolean;
}

export interface ReviewDocumentsResponse {
  documents: ReviewDocument[];
}

export interface LoanDocument {
  documentType: string;
  agreedAt: string;
}

export interface EvaluationRequest {
  requestKey: string;
  bankCode: string;
  customerName: string;
  customerRrnPrefix: string;
  customerPhone: string;
  depositBankCode: string;
  depositAccountNo: string;
  requestedAmount?: number;
  requestedPeriod?: number;
  documents: LoanDocument[];
}

export interface EvaluationResponse {
  loanNo: string;
  status: string;
}

export interface AvailableProduct {
  loanProductCode: string;
  loanProductName: string;
  minAmount: number;
  maxAmount: number;
  interestRate: number;
  loanPeriodMonths: number;
}

export interface EvaluationStatusResponse {
  evaluationStatus: 'APPROVED' | 'REJECTED' | 'SYSTEM_ERROR';
  evaluationId: string | null;
  approvedLimit: number | null;
  rejectionMessage: string | null;
  availableProducts: AvailableProduct[] | null;
}

export interface ContractDocument {
  documentType: string;
  documentName: string;
  documentUrl: string;
  documentContent?: string;
  isMandatory: boolean;
}

export interface ContractDocumentsResponse {
  loanProductCode: string;
  loanProductName: string;
  approvedLimit: number;
  documents: ContractDocument[];
}

export interface ExecutionRequest {
  evaluationId: string;
  loanProductCode: string;
  depositAccountNo: string;
  accountPassword: string;
  executeAmount: number;
  repaymentPeriod: number;
}

export interface ExecutionResponse {
  loanId: string;
  borrowerName: string;
  depositTransactionId: string;
  loanBalance: number;
  executeAmount: number;
  interestRate: number;
  repaymentPeriod: number;
  monthlyPayment: number;
  repaymentStartDate: string;
  maturityDate: string;
}

export const fetchReviewDocuments = async (): Promise<ReviewDocumentsResponse> => {
  const { data } = await axiosInstance.get<ApiResponse<ReviewDocumentsResponse>>(
    '/loan/review/documents',
  );
  return data.data!;
};

export const submitLoanEvaluation = async (
  payload: EvaluationRequest,
  files: File[],
): Promise<EvaluationResponse> => {
  const formData = new FormData();
  formData.append(
    'data',
    new Blob([JSON.stringify(payload)], { type: 'application/json' }),
  );
  files.forEach((file) => formData.append('files', file));
  const { data } = await axiosInstance.post<ApiResponse<EvaluationResponse>>(
    '/loan/evaluation',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  return data.data!;
};

export const fetchContractDocuments = async (
  loanProductCode: string,
  loanNo: string,
): Promise<ContractDocumentsResponse> => {
  const { data } = await axiosInstance.get<ApiResponse<ContractDocumentsResponse>>(
    `/loan/contract/documents/${loanProductCode}/${loanNo}`,
  );
  return data.data!;
};

export const executeLoan = async (payload: ExecutionRequest): Promise<ExecutionResponse> => {
  const { data } = await axiosInstance.post<ApiResponse<ExecutionResponse>>(
    '/loan/contract/execution',
    payload,
  );
  return data.data!;
};

export interface BankOption {
  bankCode: string;
  bankName: string;
}

export const fetchBankList = async (): Promise<BankOption[]> => {
  const { data } = await axiosInstance.get<ApiResponse<BankOption[]>>('/banks');
  return data.data!;
};
