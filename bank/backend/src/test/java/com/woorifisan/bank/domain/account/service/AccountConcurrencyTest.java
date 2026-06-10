package com.woorifisan.bank.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.woorifisan.bank.domain.account.dto.decrypted.DecryptedWithdrawData;
import com.woorifisan.bank.domain.account.dto.request.WithdrawalRequest;
import com.woorifisan.bank.domain.account.mapper.AccountMapper;
import com.woorifisan.bank.domain.account.model.Account;
import com.woorifisan.bank.global.exception.BusinessException;
import com.woorifisan.bank.global.response.ErrorCode;
import com.woorifisan.bank.global.security.service.SecurityService;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 비관적 락(SELECT FOR UPDATE) 동시성 테스트
 *
 * 이 테스트 클래스는 @Transactional을 의도적으로 사용하지 않습니다.
 * 각 스레드가 독립적인 DB 트랜잭션을 시작해야 락 경쟁이 실제로 발생하기 때문입니다.
 * @Transactional을 붙이면 테스트 메서드 전체가 하나의 트랜잭션으로 묶여
 * 스레드 간 락 경쟁을 재현할 수 없습니다.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class AccountConcurrencyTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PlatformTransactionManager txManager;

    @MockitoBean
    private SecurityService securityService;

    // ── 테스트 픽스처 ──────────────────────────────────────────────

    private void mockSecurityService(String accountNo) {
        DecryptedWithdrawData data = DecryptedWithdrawData.builder()
                .withdrawalAccountNo(accountNo)
                .customerRrnPrefix("rrn-200")
                .withdrawalPassword("123456")
                .customerName("동시성테스터")
                .build();
        SecurityService.DecryptionResult<DecryptedWithdrawData> result =
                new SecurityService.DecryptionResult<>(data, mock(SecretKey.class));
        given(securityService.decryptWithKey(any(), eq(DecryptedWithdrawData.class)))
                .willReturn(result);
        given(securityService.encryptResponse(any(), any(SecretKey.class)))
                .willReturn("encrypted-payload");
    }

    private WithdrawalRequest requestOf(BigDecimal amount) {
        return WithdrawalRequest.builder()
                .reqPayload("dummy-jwe")
                .bankKeyId("test-key")
                .amount(amount)
                .build();
    }

    // ── 1. 비관적 락 적용 시 올바른 동작 ──────────────────────────

    @Nested
    @DisplayName("[비관적 락 ON] WithdrawalService 동시성 시나리오")
    class WithPessimisticLock {

        /**
         * 시나리오: 잔액 10,000원짜리 계좌에 2개 스레드가 동시에 10,000원 출금을 시도한다.
         *
         * <p>비관적 락 없이 초기 잔액 확인만 하면 두 스레드 모두
         * "잔액 충분" 판단을 통과해 이중 출금(더블 스펜딩)이 발생한다.
         * findByIdForUpdate 재조회 후 재검증 덕분에 하나만 성공해야 한다.
         *
         * <p>기대 결과:
         * - 성공: 1건, 잔액부족 실패: 1건
         * - 최종 잔액: 0원 (음수 불허)
         */
        @Test
        @DisplayName("더블 스펜딩 방지: 잔액=출금액인 계좌에 2스레드 동시 출금 → 1건만 성공")
        @Sql(scripts = "/sql/account-concurrency-test.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = "/sql/account-concurrency-cleanup.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void 더블스펜딩_방지() throws InterruptedException {
            mockSecurityService("acc-201");

            int threadCount = 2;
            BigDecimal withdrawAmount = new BigDecimal("10000.00"); // 잔액과 동일
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger insufficientCount = new AtomicInteger(0);

            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        withdrawalService.withdraw(requestOf(withdrawAmount));
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getErrorCode() == ErrorCode.INSUFFICIENT_BALANCE) {
                            insufficientCount.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("스레드 준비 타임아웃: 10초 내에 모든 스레드가 준비되지 않았습니다").isTrue();
            start.countDown(); // 모든 스레드 동시 출발
            done.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Account account = accountMapper.findByAccountNoPlain("acc-201").get();
            log.info("[더블스펜딩 방지] 성공: {}건, 잔액부족: {}건, 최종잔액: {}",
                    successCount.get(), insufficientCount.get(), account.getBalance());

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(insufficientCount.get()).isEqualTo(1);
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * 시나리오: 잔액 100,000원짜리 계좌에 10개 스레드가 동시에 10,000원씩 출금한다.
         *
         * <p>비관적 락이 없으면 Lost Update가 발생해 일부 차감이 누락된다.
         * 락이 있으면 트랜잭션이 직렬화되어 최종 잔액이 정확하게 차감된다.
         *
         * <p>기대 결과:
         * - 성공한 출금 건수 × 10,000 = 100,000 - 최종잔액
         * - 최종 잔액 ≥ 0 (잔액 초과 출금 불허)
         */
        @Test
        @DisplayName("Lost Update 방지: 10개 스레드 동시 출금 → 최종 잔액이 정확히 차감됨")
        @Sql(scripts = "/sql/account-concurrency-test.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = "/sql/account-concurrency-cleanup.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void LostUpdate_방지() throws InterruptedException {
            mockSecurityService("acc-202");

            int threadCount = 10;
            BigDecimal withdrawAmount = new BigDecimal("10000.00");
            BigDecimal initialBalance = new BigDecimal("100000.00");
            AtomicInteger successCount = new AtomicInteger(0);

            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        withdrawalService.withdraw(requestOf(withdrawAmount));
                        successCount.incrementAndGet();
                    } catch (BusinessException ignored) {
                        // 잔액 부족 예외는 정상 — 성공 카운트에 포함하지 않음
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("스레드 준비 타임아웃: 10초 내에 모든 스레드가 준비되지 않았습니다").isTrue();
            start.countDown();
            done.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Account account = accountMapper.findByAccountNoPlain("acc-202").get();
            BigDecimal expectedBalance = initialBalance
                    .subtract(withdrawAmount.multiply(BigDecimal.valueOf(successCount.get())));

            log.info("[Lost Update 방지] 성공: {}건, 최종잔액: {}, 기대잔액: {}",
                    successCount.get(), account.getBalance(), expectedBalance);

            // 잔액은 절대 음수가 되면 안 됨
            assertThat(account.getBalance())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);

            // Lost Update가 없으면 성공건수 × 출금액 = 실제 차감액
            assertThat(account.getBalance())
                    .isEqualByComparingTo(expectedBalance);
        }

        /**
         * 시나리오: 잔액보다 큰 금액을 5개 스레드가 동시에 출금 시도한다.
         *
         * <p>초기 잔액 10,000원 계좌에 각 스레드가 6,000원씩 출금을 시도한다.
         * 비관적 락 없이 처음 검증만 보면 모두 통과하겠지만,
         * 락 재획득 후 재검증이 있으므로 최대 1건만 성공할 수 있다.
         *
         * <p>기대 결과:
         * - 최종 잔액 ≥ 0 (음수 절대 불허)
         * - 성공 건수 × 6,000 + 최종잔액 = 10,000
         */
        @Test
        @DisplayName("잔액 음수 방지: 잔액보다 큰 금액을 5개 스레드 동시 시도 → 잔액은 0 이상")
        @Sql(scripts = "/sql/account-concurrency-test.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = "/sql/account-concurrency-cleanup.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void 잔액음수_방지() throws InterruptedException {
            mockSecurityService("acc-201");

            int threadCount = 5;
            BigDecimal withdrawAmount = new BigDecimal("6000.00"); // 잔액(10,000)보다 작지만 합산하면 초과
            BigDecimal initialBalance = new BigDecimal("10000.00");
            AtomicInteger successCount = new AtomicInteger(0);

            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        withdrawalService.withdraw(requestOf(withdrawAmount));
                        successCount.incrementAndGet();
                    } catch (BusinessException ignored) {
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("스레드 준비 타임아웃: 10초 내에 모든 스레드가 준비되지 않았습니다").isTrue();
            start.countDown();
            done.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Account account = accountMapper.findByAccountNoPlain("acc-201").get();
            BigDecimal expectedBalance = initialBalance
                    .subtract(withdrawAmount.multiply(BigDecimal.valueOf(successCount.get())));

            log.info("[잔액음수 방지] 성공: {}건, 최종잔액: {}", successCount.get(), account.getBalance());

            assertThat(account.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
        }
    }

    // ── 2. 비관적 락 없을 때 경쟁 조건 발생 재현 ──────────────────

    @Nested
    @DisplayName("[비관적 락 OFF] 경쟁 조건 발생 재현 — 락의 필요성 입증")
    class WithoutPessimisticLock {

        /**
         * 시나리오: SELECT FOR UPDATE 없이 잔액을 읽고 업데이트하면 더블 스펜딩이 발생한다.
         *
         * <p>두 스레드가 동시에 잔액을 읽고(둘 다 10,000원 확인) 각자 10,000원을 차감하면
         * 최종 잔액이 -10,000원이 된다.
         *
         * 실행 흐름:
         * 1. Thread1 - findById balance=10,000 (잠금 없음)
         * 2. Thread2 - findById balance=10,000 (잠금 없음, 동시에 읽음)
         * 3. Thread1 - 잔액 충분 판단 → updateBalance(-10,000) → balance=0 커밋
         * 4. Thread2 - 잔액 충분 판단 → updateBalance(-10,000) → balance=-10,000 커밋  ← 더블 스펜딩!
         *
         * <p>이 테스트는 balance < 0 이 됨을 assertThat으로 검증해
         * 비관적 락이 없으면 동시성 문제가 실제로 발생함을 증명합니다.
         */
        @Test
        @DisplayName("경쟁 조건 재현: 락 없이 동시 출금 → 잔액이 음수가 됨 (비관적 락 필요성 입증)")
        @Sql(scripts = "/sql/account-concurrency-test.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = "/sql/account-concurrency-cleanup.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void 락없으면_더블스펜딩_발생() throws InterruptedException {
            final long targetAccountId = 203L;
            final BigDecimal withdrawAmount = new BigDecimal("10000.00");
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);

            AtomicInteger successCount = new AtomicInteger(0);

            // 두 스레드가 잔액을 동시에 읽도록 동기화
            CountDownLatch bothRead = new CountDownLatch(2);
            CountDownLatch startUpdate = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    txTemplate.execute(status -> {
                        // 1단계: 잠금 없이 잔액 조회 (findById — FOR UPDATE 미사용)
                        Account acc = accountMapper.findById(targetAccountId).get();
                        bothRead.countDown(); // "나도 읽었다" 신호

                        try {
                            boolean signaled = startUpdate.await(5, TimeUnit.SECONDS);
                            if (!signaled) {
                                // 타임아웃: 동기화가 실패했으므로 트랜잭션을 중단
                                status.setRollbackOnly();
                                return null;
                            }
                        } catch (InterruptedException e) {
                            // 인터럽트: 즉시 중단
                            Thread.currentThread().interrupt();
                            status.setRollbackOnly();
                            return null;
                        }

                        // 2단계: 둘 다 잔액을 10,000으로 읽었으므로 둘 다 "충분" 판단 → 차감
                        if (acc.getBalance().compareTo(withdrawAmount) >= 0) {
                            accountMapper.updateBalance(targetAccountId, withdrawAmount.negate());
                            successCount.incrementAndGet();
                        }
                        return null;
                    });
                    done.countDown();
                });
            }

            // 두 스레드가 모두 잔액을 읽은 뒤 동시에 업데이트 시작
            bothRead.await(5, TimeUnit.SECONDS);
            startUpdate.countDown();
            done.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Account finalAccount = accountMapper.findById(targetAccountId).get();
            log.info("[경쟁 조건 재현] 성공: {}건, 최종잔액: {} (음수 = 더블 스펜딩 발생)",
                    successCount.get(), finalAccount.getBalance());

            // 락 없이 두 스레드가 모두 성공했으므로 잔액이 음수
            assertThat(successCount.get()).isEqualTo(2);
            assertThat(finalAccount.getBalance())
                    .isLessThan(BigDecimal.ZERO); // -10,000원이 됨 → 비관적 락 필요성 입증
        }

        /**
         * 시나리오: 비관적 락이 있으면 같은 상황에서 하나만 성공한다.
         *
         * <p>위 테스트('락없으면_더블스펜딩_발생')와 동일한 실행 흐름이지만
         * findByIdForUpdate(SELECT FOR UPDATE)를 사용한다.
         *
         * 실행 흐름:
         * 1. Thread1 - findByIdForUpdate → 행 잠금 획득
         * 2. Thread2 - findByIdForUpdate → Thread1 커밋까지 BLOCK
         * 3. Thread1 - updateBalance(-10,000) → balance=0, 커밋 (락 해제)
         * 4. Thread2 - 잠금 획득, 재조회 → balance=0, 0 < 10,000 → 차감 포기
         *
         * <p>기대 결과: 성공 1건, 최종 잔액 0 (음수 없음)
         */
        @Test
        @DisplayName("비교 검증: FOR UPDATE 사용 시 동일 상황에서 잔액이 음수가 되지 않음")
        @Sql(scripts = "/sql/account-concurrency-test.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = "/sql/account-concurrency-cleanup.sql",
             config = @SqlConfig(encoding = "UTF-8"),
             executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void 락있으면_더블스펜딩_불발() throws InterruptedException {
            final long targetAccountId = 203L;
            final BigDecimal withdrawAmount = new BigDecimal("10000.00");
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);

            AtomicInteger successCount = new AtomicInteger(0);

            CountDownLatch done = new CountDownLatch(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    txTemplate.execute(status -> {
                        // SELECT FOR UPDATE — 한 스레드만 진입, 나머지는 대기
                        Account acc = accountMapper.findByIdForUpdate(targetAccountId).get();

                        if (acc.getBalance().compareTo(withdrawAmount) >= 0) {
                            accountMapper.updateBalance(targetAccountId, withdrawAmount.negate());
                            successCount.incrementAndGet();
                        }
                        return null;
                    });
                    done.countDown();
                });
            }

            done.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Account finalAccount = accountMapper.findById(targetAccountId).get();
            log.info("[비교 검증] 성공: {}건, 최종잔액: {}", successCount.get(), finalAccount.getBalance());

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(finalAccount.getBalance())
                    .isEqualByComparingTo(BigDecimal.ZERO); // 정확히 0원
        }
    }
}