package com.woorifisan.platform.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * SSE 전용 스레드 풀
     *
     * CompletableFuture.runAsync()의 기본 풀인 ForkJoinPool.commonPool()은
     * CPU-bound 단기 작업용이라, Thread.sleep이나 외부 API 대기처럼 스레드를 오래
     * 점유하는 SSE 작업에 사용하면 다른 비동기 작업까지 지연된다.
     * 전용 풀을 분리해 SSE가 공용 풀을 잠식하지 않도록 한다.
     *
     * corePoolSize  5  : 평시 동시 SSE 연결 처리 수
     * maxPoolSize  20  : 피크 시 최대 스레드 수
     * queueCapacity 50 : 풀이 꽉 찼을 때 대기 가능한 태스크 수
     * awaitTermination 30s : SseEmitter 타임아웃(30s)과 맞춰 graceful shutdown 보장
     */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sse-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
