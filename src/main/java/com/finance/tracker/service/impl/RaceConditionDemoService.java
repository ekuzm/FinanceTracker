package com.finance.tracker.service.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RaceConditionDemoService {

    private static final int THREAD_COUNT = 50;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_VALUE = THREAD_COUNT * INCREMENTS_PER_THREAD;

    public void demonstrateRaceCondition() throws InterruptedException {
        log.info("RACE CONDITION:");
        DemoCounters counters = new DemoCounters();
        executeConcurrentIncrements(counters::incrementUnsafe);
        int actualValue = counters.getUnsafeValue();
        logResults("Unsafe counter", actualValue);
    }

    public void demonstrateSynchronizedSolution() throws InterruptedException {
        log.info("SYNCHRONIZED:");
        DemoCounters counters = new DemoCounters();
        executeConcurrentIncrements(counters::incrementSynchronized);
        int actualValue = counters.getSynchronizedValue();
        logResults("Synchronized counter", actualValue);
    }

    public void demonstrateAtomicSolution() throws InterruptedException {
        log.info("ATOMICINTEGER:");
        DemoCounters counters = new DemoCounters();
        executeConcurrentIncrements(counters::incrementAtomic);
        int actualValue = counters.getAtomicValue();
        logResults("Atomic counter", actualValue);
    }

    public void runAllDemos() throws InterruptedException {
        log.info("Starting race condition demonstration with {} threads", THREAD_COUNT);
        log.info("Each thread performs {} increments", INCREMENTS_PER_THREAD);
        log.info("Expected value: {}", EXPECTED_VALUE);

        demonstrateRaceCondition();
        demonstrateSynchronizedSolution();
        demonstrateAtomicSolution();
    }

    private void executeConcurrentIncrements(Runnable incrementAction) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    incrementAction.run();
                }
            });
        }

        awaitTermination(executor);
    }

    private void awaitTermination(ExecutorService executor) throws InterruptedException {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                throw new IllegalStateException("Failed to complete race-condition demo within 1 minute");
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void logResults(String counterName, int actualValue) {
        log.info("{}:", counterName);
        log.info("  Expected value: {}", EXPECTED_VALUE);
        log.info("  Actual value: {}", actualValue);

        if (counterName.equals("Unsafe counter")) {
            log.info("  Lost updates: {}", EXPECTED_VALUE - actualValue);
            log.info("  Race condition: {}", actualValue != EXPECTED_VALUE ? "PRESENT" : "ABSENT");
        } else {
            log.info("  Result: {}", actualValue == EXPECTED_VALUE ? "SUCCESS" : "FAILURE");
        }
    }

    private static final class DemoCounters {

        private final AtomicInteger atomicCounter = new AtomicInteger();
        private int unsafeCounter;
        private int synchronizedCounter;

        private void incrementUnsafe() {
            unsafeCounter++;
        }

        private int getUnsafeValue() {
            return unsafeCounter;
        }

        private synchronized void incrementSynchronized() {
            synchronizedCounter++;
        }

        private synchronized int getSynchronizedValue() {
            return synchronizedCounter;
        }

        private void incrementAtomic() {
            atomicCounter.incrementAndGet();
        }

        private int getAtomicValue() {
            return atomicCounter.get();
        }
    }
}
