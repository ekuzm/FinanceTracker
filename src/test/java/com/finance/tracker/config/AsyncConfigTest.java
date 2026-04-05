package com.finance.tracker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutorShouldCreateInitializedThreadPoolExecutor() throws Exception {
        Executor executor = asyncConfig.getAsyncExecutor();

        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        try {
            assertEquals(5, taskExecutor.getCorePoolSize());
            assertEquals(10, taskExecutor.getMaxPoolSize());
            assertEquals("AsyncTask-", taskExecutor.getThreadNamePrefix());
            assertNotNull(taskExecutor.getThreadPoolExecutor());
            assertEquals(25, taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity());

            CompletableFuture<String> threadName = new CompletableFuture<>();
            taskExecutor.execute(() -> threadName.complete(Thread.currentThread().getName()));

            assertTrue(threadName.get(5, TimeUnit.SECONDS).startsWith("AsyncTask-"));
        } finally {
            taskExecutor.shutdown();
        }
    }
}
