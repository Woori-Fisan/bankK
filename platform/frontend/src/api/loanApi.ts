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

/**
 * 대출 심사 신청 요청 (E2EE 적용 전 원본 데이터 구조)
 * prepareSecureRequest에 의해 reqPayload로 변환됩니다.
 */
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
  resPayload?: string;
}

export interface AvailableProduct {
  loanProductCode: string;
  loanProductName: string;
  minAmount: number;
  maxAmount: number;
  interestRate: number;
  loanPeriodMonths: number;
}

/**
 * SSE로 push되는 심사 결과 (E2EE 적용)
 * 민감 정보는 resPayload에 암호화되어 담깁니다.
 */
export interface EvaluationStatusResponse {
  evaluationStatus: 'APPROVED' | 'REJECTED' | 'SYSTEM_ERROR';
  evaluationId: string | null;
  approvedLimit?: number | null;
  rejectionMessage?: string | null;
  availableProducts?: AvailableProduct[] | null;
  resPayload?: string;
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

/**
 * 대출 실행 요청 (E2EE 적용 전 원본 데이터 구조)
 */
export interface ExecutionRequest {
  loanNo: string;
  productId: number;
  depositAccountNo: string;
  accountPassword: string;
  executeAmount: number;
  repaymentPeriod: number;
  repaymentType: string;
}

export interface ExecutionResponse {
  loanNo: string;
  executeAmount: number;
  interestRate: number;
  repaymentPeriod: number;
  monthlyPayment: number;
  repaymentType: string;
  startDate: string;
  maturityDate: string;
  linkedAccountId: number;
  resPayload?: string;
  borrowerName?: string; // 복호화된 고객 성명을 담을 필드 추가
}

export const fetchReviewDocuments = async (): Promise<ReviewDocumentsResponse> => {
  const { data } = await axiosInstance.get<ApiResponse<ReviewDocumentsResponse>>(
    '/loan/review/documents',
  );
  return data.data!;
};

export const submitLoanEvaluation = async (
  payload: any,
  files: File[],
  headers?: Record<string, string>
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
    { headers: { 'Content-Type': 'multipart/form-data', ...headers } },
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

export const executeLoan = async (
  payload: any,
  headers?: Record<string, string>
): Promise<ExecutionResponse> => {
  const { data } = await axiosInstance.post<ApiResponse<ExecutionResponse>>(
    '/loan/contract/execution',
    payload,
    { headers }
  );
  return data.data!;
};

export interface ReceiptRequest {
  loanId: string;
  borrowerName: string;
  depositTransactionId: string;
  executeAmount: number;
  interestRate: number;
  repaymentPeriod: number;
  monthlyPayment: number;
  repaymentStartDate: string;
  maturityDate: string;
  loanProductName: string;
  depositBankName: string;
  depositAccountNo: string;
}

export const downloadLoanReceipt = async (payload: ReceiptRequest): Promise<Blob> => {
  const { data } = await axiosInstance.post('/loan/receipt', payload, {
    responseType: 'blob',
  });
  return data;
};

export interface BankOption {
  bankCode: string;
  bankName: string;
}

export const fetchBankList = async (): Promise<BankOption[]> => {
  const { data } = await axiosInstance.get<ApiResponse<BankOption[]>>('/banks');
  return data.data!;
};

// SSE 연결 실패 시 Redis 캐시에서 결과를 직접 조회하는 polling fallback
export const fetchLoanResult = async (
  requestKey: string,
): Promise<EvaluationStatusResponse | null> => {
  const response = await axiosInstance.get<ApiResponse<EvaluationStatusResponse>>(
    `/loan/result?requestKey=${encodeURIComponent(requestKey)}`,
    { validateStatus: (status) => status === 200 || status === 204 },
  );
  if (response.status === 204) return null;
  return response.data.data!;
};
