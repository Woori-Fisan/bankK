import axiosInstance from './axiosInstance';
import type { ApiResponse } from '../types/common';

export interface ReviewDocument {
  documentType: string;
  documentName: string;
  documentUrl: string;
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
  bankCode: string;
  customerName: string;
  customerRrnPrefix: string;
  customerPhone: string;
  depositBankCode: string;
  depositAccountNo: string;
  documents: LoanDocument[];
}

export interface EvaluationResponse {
  applicationId: string;
  receivedAt: string;
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
  applicationId: string;
  evaluationStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'FAILED';
  requestedAt: string;
  completedAt: string | null;
  evaluationId: string | null;
  approvedLimit: number | null;
  rejectionCode: string | null;
  rejectionMessage: string | null;
  availableProducts: AvailableProduct[] | null;
}

export interface ContractDocument {
  documentType: string;
  documentName: string;
  documentUrl: string;
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
): Promise<EvaluationResponse> => {
  const { data } = await axiosInstance.post<ApiResponse<EvaluationResponse>>(
    '/loan/evaluation',
    payload,
  );
  return data.data!;
};

export const fetchContractDocuments = async (
  loanProductCode: string,
  evaluationId: string,
): Promise<ContractDocumentsResponse> => {
  const { data } = await axiosInstance.get<ApiResponse<ContractDocumentsResponse>>(
    `/loan/contract/documents/${loanProductCode}/${evaluationId}`,
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
