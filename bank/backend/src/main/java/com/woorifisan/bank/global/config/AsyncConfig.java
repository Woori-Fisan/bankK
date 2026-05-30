package com.woorifisan.bank.global.config;

import java.util.concurrent.Executor;
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
        executor.setCorePoolSize(5);      // 평소 대기 스레드 수
        executor.setMaxPoolSize(20);      // 큐가 꽉 찼을 때 최대로 늘어날 수 있는 스레드 수
        executor.setQueueCapacity(100);   // 스레드가 모두 바쁠 때 대기시킬 수 있는 작업 수
        executor.setThreadNamePrefix("loan-review-"); // 로그에서 스레드 식별용
        executor.initialize();
        return executor;
    }
}
