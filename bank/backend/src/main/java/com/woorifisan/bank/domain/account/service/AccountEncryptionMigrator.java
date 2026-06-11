package com.woorifisan.bank.domain.account.service;

import com.woorifisan.bank.global.util.CryptoUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기존 평문 계좌번호를 AES 암호화 및 SHA-256 해싱(Blind Index)으로 마이그레이션하는 컴포넌트
 * 애플리케이션 시작 시 자동으로 실행됩니다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AccountEncryptionMigrator implements ApplicationRunner {

    private final CryptoUtil cryptoUtil;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateNow();
    }

    /**
     * 마이그레이션 실행 로직
     * account_no_hash가 null인 레코드를 대상으로 암호화 및 해싱을 수행합니다.
     */
    @Transactional
    public void migrateNow() {
        // 마이그레이션 대상 조회
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, account_no FROM account WHERE account_no_hash IS NULL AND account_no IS NOT NULL"
        );

        if (rows.isEmpty()) {
            log.info("AccountEncryptionMigrator: 마이그레이션 대상 없음 (이미 완료)");
            return;
        }

        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String accountNo = (String) row.get("account_no");

            // 1. AES-256-GCM 암호화
            String enc  = cryptoUtil.encrypt(accountNo);
            // 2. SHA-256 해싱 (Blind Index)
            String hash = cryptoUtil.hash(accountNo);

            // 3. DB 업데이트
            jdbcTemplate.update(
                    "UPDATE account SET account_no_enc = ?, account_no_hash = ? WHERE id = ?",
                    enc, hash, id
            );
        }

        log.info("AccountEncryptionMigrator: {}건 마이그레이션 완료", rows.size());
    }
}