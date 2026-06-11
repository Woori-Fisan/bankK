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

    @Transactional
    public void migrateNow() {
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

            String enc  = cryptoUtil.encrypt(accountNo);
            String hash = cryptoUtil.hash(accountNo);

            jdbcTemplate.update(
                    "UPDATE account SET account_no_enc = ?, account_no_hash = ? WHERE id = ?",
                    enc, hash, id
            );
        }

        log.info("AccountEncryptionMigrator: {}건 마이그레이션 완료", rows.size());
    }
}
