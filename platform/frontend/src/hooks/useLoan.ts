import { useEffect, useState } from 'react';
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
  type EvaluationStatusResponse,
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
    mutationFn: (payload: EvaluationRequest) => submitLoanEvaluation(payload),
  });

export const useEvaluationSSE = (applicationId: string | null) => {
  const [data, setData] = useState<EvaluationStatusResponse | null>(null);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!applicationId) return;

    const es = new EventSource(`/api/v1/loan/evaluation/${applicationId}/stream`);

    es.addEventListener('status', (event) => {
      try {
        const parsed: EvaluationStatusResponse = JSON.parse(event.data);
        setData(parsed);
        if (parsed.evaluationStatus !== 'PENDING') {
          es.close();
        }
      } catch {
        setError(new Error('응답 파싱 오류'));
        es.close();
      }
    });

    es.onerror = () => {
      setError(new Error('심사 결과 조회 중 연결 오류가 발생했습니다.'));
      es.close();
    };

    return () => es.close();
  }, [applicationId]);

  return { data, error };
};

export const useContractDocuments = (
  loanProductCode: string | null,
  evaluationId: string | null,
) =>
  useQuery({
    queryKey: ['loan', 'contract', 'documents', loanProductCode, evaluationId],
    queryFn: () => fetchContractDocuments(loanProductCode!, evaluationId!),
    enabled: !!loanProductCode && !!evaluationId,
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
