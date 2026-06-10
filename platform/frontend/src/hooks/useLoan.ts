import { useQuery, useMutation } from '@tanstack/react-query';
import {
  fetchReviewDocuments,
  submitLoanEvaluation,
  fetchContractDocuments,
  executeLoan,
  fetchBankList,
} from '../api/loanApi';
import { extractApiErrorMessage } from '../utils/apiError';

export const extractApiError = (error: unknown): string => {
  return extractApiErrorMessage(error);
};

export const useReviewDocuments = () =>
  useQuery({
    queryKey: ['loan', 'review', 'documents'],
    queryFn: fetchReviewDocuments,
  });

export const useSubmitLoanEvaluation = () =>
  useMutation({
    mutationFn: ({
      payload,
      files,
      headers,
    }: {
      payload: any;
      files: File[];
      headers?: Record<string, string>;
    }) => submitLoanEvaluation(payload, files, headers),
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
    mutationFn: ({ payload, headers }: { payload: any; headers?: Record<string, string> }) =>
      executeLoan(payload, headers),
  });

export const useBankList = () =>
  useQuery({
    queryKey: ['banks'],
    queryFn: fetchBankList,
    staleTime: Infinity,
  });
