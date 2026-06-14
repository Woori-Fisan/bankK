import { useEffect, useRef } from 'react';
import { getTransferStatus } from '../api/transfer';

const MAX_POLLS = 20;
const INITIAL_DELAY_MS = 2000;
const POLL_INTERVAL_MS = 3000;
const MAX_CONSECUTIVE_NETWORK_ERRORS = 3;

interface UseTransferPollingOptions {
    transactionId: string;
    withdrawalBankCode: string;
    onSuccess: () => void;
    onFailed: () => void;
    onTimeout: () => void;
}

export function useTransferPolling({
    transactionId,
    withdrawalBankCode,
    onSuccess,
    onFailed,
    onTimeout,
}: UseTransferPollingOptions) {
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const isActiveRef = useRef(false);
    const pollCountRef = useRef(0);
    const networkErrorCountRef = useRef(0);

    const paramsRef = useRef({ transactionId, withdrawalBankCode });
    const callbacksRef = useRef({ onSuccess, onFailed, onTimeout });

    useEffect(() => {
        paramsRef.current = { transactionId, withdrawalBankCode };
    });
    useEffect(() => {
        callbacksRef.current = { onSuccess, onFailed, onTimeout };
    });

    useEffect(() => {
        isActiveRef.current = true;
        pollCountRef.current = 0;
        networkErrorCountRef.current = 0;

        const poll = async () => {
            if (!isActiveRef.current) return;

            if (pollCountRef.current >= MAX_POLLS) {
                isActiveRef.current = false;
                callbacksRef.current.onTimeout();
                return;
            }

            try {
                const { transactionId, withdrawalBankCode } = paramsRef.current;
                const response = await getTransferStatus(withdrawalBankCode, transactionId);

                if (!isActiveRef.current) return;

                networkErrorCountRef.current = 0;
                pollCountRef.current++;

                if (response.data.status === 'SUCCESS') {
                    isActiveRef.current = false;
                    callbacksRef.current.onSuccess();
                } else if (response.data.status === 'FAILED') {
                    isActiveRef.current = false;
                    callbacksRef.current.onFailed();
                } else {
                    // PENDING: 다음 폴링 예약
                    timeoutRef.current = setTimeout(poll, POLL_INTERVAL_MS);
                }
            } catch {
                if (!isActiveRef.current) return;
                networkErrorCountRef.current++;
                if (networkErrorCountRef.current >= MAX_CONSECUTIVE_NETWORK_ERRORS) {
                    isActiveRef.current = false;
                    callbacksRef.current.onTimeout();
                } else {
                    // 네트워크 오류: 횟수 차감 없이 재시도
                    timeoutRef.current = setTimeout(poll, POLL_INTERVAL_MS);
                }
            }
        };

        timeoutRef.current = setTimeout(poll, INITIAL_DELAY_MS);

        return () => {
            isActiveRef.current = false;
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
                timeoutRef.current = null;
            }
        };
    }, []); // 마운트 시 한 번만 실행, 언마운트 시 자동 정리
}
