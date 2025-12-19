package com.example.pizza.config.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 *
 * Spring @Async için thread pool yapılandırması.
 * - Non-blocking email gönderimi
 * - Non-blocking file upload (Cloudinary)
 * - AsyncConfigConstants'tan gelen değerler
 *
 * FIXED: proxyTargetClass=true eklendi
 * Bu, Spring'in JDK proxy yerine CGLib proxy kullanmasını sağlar.
 * FileUploadImpl gibi concrete class'lar doğru inject edilebilir.
 */
@Slf4j
@Configuration
@EnableAsync(proxyTargetClass = true)  // FIXED: Force CGLib-based proxies
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Email işlemleri için async executor
     * - Küçük thread pool (email gönderimi CPU intensive değil)
     * - Queue capacity yüksek (email'ler sıraya girebilir)
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // AsyncConfigConstants kullanarak yapılandır
        executor.setCorePoolSize(2);  // Email için küçük pool
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.setKeepAliveSeconds(60);

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Email Task Executor initialized: core={}, max={}, queue={}",
                2, 5, 100);

        return executor;
    }

    /**
     * File upload işlemleri için async executor
     * - Daha büyük thread pool (I/O intensive)
     * - Cloudinary upload işlemleri için
     */
    @Bean(name = "fileUploadTaskExecutor")
    public Executor fileUploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // AsyncConfigConstants kullanarak yapılandır
        executor.setCorePoolSize(3);  // File upload için daha büyük pool
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("file-");
        executor.setKeepAliveSeconds(60);

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("File Upload Task Executor initialized: core={}, max={}, queue={}",
                3, 10, 50);

        return executor;
    }


    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    /**
     * Async exception handler
     * @Async metodlarında yakalanmayan exception'ları loglar
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Async method '{}' threw exception: {}",
                        method.getName(), ex.getMessage(), ex);
                log.error("Method params: {}", (Object) params);
            }
        };
    }
}