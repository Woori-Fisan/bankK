import { useQuery, useMutation } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import {
  fetchReviewDocuments,
  submitLoanEvaluation,
  fetchContractDocuments,
  executeLoan,
  fetchBankList,
  type EvaluationRequest,
  type ExecutionRequest,
} from '../api/loanApi';
import type { ApiResponse } from '../types/common';

export const extractApiError = (error: unknown): string => {
  if (error instanceof AxiosError && error.response?.data) {
    const body = error.response.data as ApiResponse;
    if (body.error?.message) return body.error.message;
  }
  return '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
};

export const useReviewDocuments = () =>
  useQuery({
    queryKey: ['loan', 'review', 'documents'],
    queryFn: fetchReviewDocuments,
  });

export const useSubmitLoanEvaluation = () =>
  useMutation({
    mutationFn: ({ payload, files }: { payload: EvaluationRequest; files: File[] }) =>
      submitLoanEvaluation(payload, files),
  });

export const useContractDocuments = (
  loanProductCode: string | null,
  loanNo: string | null,
) =>
  useQuery({
    queryKey: ['loan', 'contract', 'documents', loanProductCode, loanNo],
    queryFn: () => fetchContractDocuments(loanProductCode!, loanNo!),
    enabled: !!loanProductCode && !!loanNo,
  });

export const useExecuteLoan = () =>
  useMutation({
    mutationFn: (payload: ExecutionRequest) => executeLoan(payload),
  });

export const useBankList = () =>
  useQuery({
    queryKey: ['banks'],
    queryFn: fetchBankList,
    staleTime: Infinity,
  });
