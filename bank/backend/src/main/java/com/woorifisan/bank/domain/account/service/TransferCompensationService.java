package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.InternalDepositRequest;
import com.woorifisan.bank.domain.account.dto.request.TransferRequest;
import com.woorifisan.bank.domain.account.dto.response.TransferStatusResponse;
import com.woorifisan.bank.global.config.BankNetworkConfig;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ApiResponse;
import com.woorifisan.bank.global.response.ErrorCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * 타행 이체 보상 처리 서비스
 * - 타행 입금 시도 및 실패 시 상태 폴링을 비동기로 수행합니다.
 * - Tomcat 스레드를 점유하지 않고 전용 스레드풀(transferCompensationExecutor)에서 실행됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferCompensationService {

    @Qualifier("bankToBankWebClient")
    private final WebClient webClient;
    private final BankNetworkConfig bankNetworkConfig;
    private final TransferTxService transferTxService;

    private static final int RETRY_MAX_ATTEMPTS = 9;
    private static final Duration RETRY_DELAY    = Duration.ofSeconds(1);
    private static final Duration BLOCK_TIMEOUT  = Duration.ofSeconds(70);

    /**
     * 타행 입금 시도 → 실패 시 상태 폴링 → 원장 직접 업데이트 또는 환불
     * 클라이언트는 PENDING 응답을 받은 후 /transfer/status/{txId}로 폴링합니다.
     */
    @Async("transferCompensationExecutor") // 별도 스레드풀에서 실행
    public void compensate(
            String depositBankCode,
            InternalDepositRequest depositRequest,
            TransferRequest originalRequest,
            DecryptedWithdrawData decryptedData,
            String txId) {

        // 1. 타행 입금 시도
        boolean depositSucceeded = false;
        try {
            callExternalDeposit(depositBankCode, depositRequest, txId);
            depositSucceeded = true;
        } catch (Exception depositEx) {
            log.error("타행 입금 실패, 상태 폴링으로 전환 - 거래ID: {}, 오류: {}", txId, depositEx.getMessage());
        }

        // 2. 입금 API 성공 → 원장 SUCCESS 업데이트 후 완료 (환불 금지)
        if (depositSucceeded) {
            try {
                transferTxService.updateLedgerStatus(txId, "SUCCESS");
                log.info("타행 입금 성공, 원장 SUCCESS 업데이트 완료 - 거래ID: {}", txId);
            } catch (Exception ledgerEx) {
                // 입금은 완료됐으므로 환불하면 이중 지급 발생. PENDING 유지하고 관리자 수동 복구 필요
                log.error("[수동 복구 필요] 타행 입금 완료 후 원장 SUCCESS 업데이트 실패 - 거래ID: {}", txId, ledgerEx);
            }
            return;
        }

        // 3. 입금 API 실패 → 상태 폴링으로 실제 처리 여부 확인
        try {
            boolean isProcessed = pollTransferStatus(depositBankCode, txId);

            if (isProcessed) {
                // 폴링 결과 입금 확인 → 원장 SUCCESS (환불 금지)
                try {
                    transferTxService.updateLedgerStatus(txId, "SUCCESS");
                    log.info("타행 거래 상태 폴링 확인: 입금 성공, 원장 SUCCESS 업데이트 완료 - 거래ID: {}", txId);
                } catch (Exception ledgerEx) {
                    log.error("[수동 복구 필요] 폴링 SUCCESS 확인 후 원장 업데이트 실패 - 거래ID: {}", txId, ledgerEx);
                }
            } else {
                // 폴링 결과 미처리 확인 → 환불 시도
                log.error("타행 거래 상태 폴링 확인: 미처리, 환불 시작 - 거래ID: {}", txId);
                try {
                    transferTxService.refundTransfer(originalRequest, decryptedData, txId);
                    log.info("환불 완료 - 거래ID: {}", txId);
                } catch (Exception refundEx) {
                    log.error("[수동 복구 필요] 환불 처리 실패 - 출금액 미회수 상태, 관리자 확인 필요 - 거래ID: {}", txId, refundEx);
                }
                // 환불 성공 여부와 무관하게 FAILED 기록 — 관리자가 FAILED 원장으로 추적·복구 가능
                safeMarkLedgerAsFailed(txId);
            }
        } catch (BusinessException statusEx) {
            // 재시도 소진 또는 비일시적 오류 → 상태 확정 불가(UNKNOWN), PENDING 유지
            log.error("[수동 복구 필요] 거래 상태 확정 불가 (UNKNOWN), PENDING 유지 - 거래ID: {}, 사유: {}", txId, statusEx.getMessage());
        } catch (Exception e) {
            log.error("[수동 복구 필요] 보상 처리 중 예기치 않은 오류 - 거래ID: {}", txId, e);
            safeMarkLedgerAsFailed(txId);
        }
    }

    /** 처리가 불확실한 경로에서 원장에 최소한 FAILED를 기록한다. 실패해도 예외를 전파하지 않는다. */
    private void safeMarkLedgerAsFailed(String txId) {
        try {
            transferTxService.updateLedgerStatus(txId, "FAILED");
        } catch (Exception e) {
            log.error("원장 FAILED 기록마저 실패, 수동 복구 필요 - 거래ID: {}", txId, e);
        }
    }

    /**
     * WebClient POST로 타행 입금 요청 (mTLS 적용)
     */
    private void callExternalDeposit(String depositBankCode, InternalDepositRequest request, String txId) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(depositBankCode);
        if (bankProperty == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND, "지원하지 않는 입금 은행입니다.");
        }

        String url = bankProperty.getInternalDepositUrl();
        log.info("타행 입금 API 호출 - URL: {}, 거래ID: {}", url, txId);

        webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientResponse::createException)
                .toBodilessEntity()
                .block(Duration.ofSeconds(5));

        log.info("타행 입금 API 호출 성공 - 거래ID: {}", txId);
    }

    /**
     * WebClient GET + retryWhen으로 타행 거래 상태 폴링 (Thread.sleep 없음)
     * - SUCCESS  → true  반환 (원장 성공 처리)
     * - FAILED   → false 반환 (환불 처리)
     * - PENDING  → 재시도
     * - 404      → 재시도 (커밋 지연 가능성)
     * - 5xx      → 재시도
     * - 비일시적 4xx → 즉시 중단 (BusinessException)
     * - 재시도 소진 → BusinessException (UNKNOWN)
     */
    private boolean pollTransferStatus(String depositBankCode, String txId) {
        BankNetworkConfig.BankProperty bankProperty = bankNetworkConfig.getBankProperty(depositBankCode);
        if (bankProperty == null) {
            throw new BusinessException(ErrorCode.BANK_NOT_FOUND, "상대 은행 설정 정보를 찾을 수 없습니다.");
        }

        String statusUrl = bankProperty.getStatusQueryUrl(txId);
        log.info("타행 거래 상태 폴링 시작 - URL: {}, 거래ID: {}", statusUrl, txId);

        Boolean result = webClient.get()
                .uri(statusUrl)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() && status.value() != 404,
                        response -> {
                            log.error("타행 거래 상태 조회 실패 (비일시적 4xx) - 거래ID: {}, 상태: {}", txId, response.statusCode().value());
                            return response.createException();
                        })
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<TransferStatusResponse>>() {})
                .flatMap(apiResponse -> {
                    String status = apiResponse.getData().getStatus();
                    log.info("타행 거래 상태 수신 - 거래ID: {}, 상태: {}", txId, status);
                    return switch (status) {
                        case "SUCCESS" -> Mono.just(true);
                        case "FAILED"  -> Mono.just(false);
                        // PENDING 등 미확정 상태 → error 방출하여 재시도 유도
                        default -> Mono.error(new PendingTransferException(status));
                    };
                })
                // 에러가 발생하면, 1초 후에 전체 파이프라인을 처음부터 재구독
                .retryWhen(Retry.fixedDelay(RETRY_MAX_ATTEMPTS, RETRY_DELAY)
                        .filter(ex ->
                                ex instanceof PendingTransferException
                                || ex instanceof WebClientRequestException // 네트워크 오류
                                || (ex instanceof WebClientResponseException wce
                                    && (wce.getStatusCode().value() == 404
                                        || wce.getStatusCode().is5xxServerError())))
                        .doBeforeRetry(signal ->
                                log.warn("타행 거래 상태 재조회 ({}회째) - 거래ID: {}, 사유: {}",
                                        signal.totalRetries() + 1, txId, signal.failure().getMessage()))
                        .onRetryExhaustedThrow((spec, signal) ->
                                new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                                        "상대 은행의 거래 처리 상태를 확정할 수 없어(UNKNOWN) 이체를 보류 상태(PENDING)로 유지합니다. 관리자 확인이 필요합니다.")))
                .block(BLOCK_TIMEOUT);

        return Boolean.TRUE.equals(result);
    }

    /** PENDING 등 미확정 상태일 때 retryWhen을 유도하기 위한 마커 예외 */
    private static class PendingTransferException extends RuntimeException {
        PendingTransferException(String status) {
            super("거래 상태 미확정: " + status, null, true, false);
        }
    }
}