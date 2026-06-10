package com.woorifisan.platform.crypto.scheduler;

import com.woorifisan.platform.crypto.service.BankKeyService;
import com.woorifisan.platform.global.config.BankNetworkConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 은행별 RSA 공개키를 주기적으로 갱신하는 스케줄러입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankKeyScheduler {

    private final BankKeyService bankKeyService;
    private final BankNetworkConfig bankNetworkConfig;

    /**
     * 매시간 정각에 모든 등록된 은행의 공개키 캐시를 갱신합니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshAllBankKeys() {
        log.info("정기 은행 공개키 캐시 갱신 작업을 시작합니다.");
        
        bankNetworkConfig.getUrls().keySet().forEach(bankCode -> {
            try {
                bankKeyService.refreshBankKey(bankCode);
            } catch (Exception e) {
                log.error("은행[{}] 공개키 갱신 중 오류 발생", bankCode, e);
            }
        });
        
        log.info("정기 은행 공개키 캐시 갱신 작업이 완료되었습니다.");
    }

    /**
     * 애플리케이션 기동 직후 최초 1회 즉시 실행하여 캐시를 초기화합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initBankKeyCache() {
        log.info("애플리케이션 초기화: 모든 은행의 공개키 캐시를 초기 생성합니다.");
        refreshAllBankKeys();
    }
}
