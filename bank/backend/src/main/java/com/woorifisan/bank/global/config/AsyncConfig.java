package com.woorifisan.bank.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync        // @Async 애노테이션 활성화
@Configuration
public class AsyncConfig {

    @Bean(name = "loanReviewExecutor")
    public Executor loanReviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("loan-review-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "transferCompensationExecutor")
    public Executor transferCompensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("transfer-comp-");
        // 큐 초과 시 호출 스레드에서 직접 실행 — 거래 보상이 유실되지 않도록 보장
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
