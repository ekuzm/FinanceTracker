package com.finance.tracker.service.impl;

import com.finance.tracker.dto.response.RaceConditionDemoResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RaceConditionDemoService {

    private static final int THREAD_COUNT = 50;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_VALUE = THREAD_COUNT * INCREMENTS_PER_THREAD;
    private static final int WORKER_READY_TIMEOUT_SECONDS = 10;

    public RaceConditionDemoResponse.CounterResult demonstrateRaceCondition() throws InterruptedException {
        log.info("UNSAFE COUNTER:");
        DemoCounters counters = new DemoCounters();
        executeUnsafeConcurrentIncrements(counters);
        RaceConditionDemoResponse.CounterResult result = buildUnsafeCounterResult(counters.getUnsafeValue());
        logCounterResult(result);
        return result;
    }

    public RaceConditionDemoResponse.CounterResult demonstrateAtomicSolution() throws InterruptedException {
        log.info("ATOMIC COUNTER:");
        DemoCounters counters = new DemoCounters();
        executeConcurrentIncrements(ignored -> counters.incrementAtomic());
        RaceConditionDemoResponse.CounterResult result =
                buildCounterResult("Atomic counter", counters.getAtomicValue());
        logCounterResult(result);
        return result;
    }

    public RaceConditionDemoResponse runAllDemos() throws InterruptedException {
        log.info("Starting race condition demonstration with {} threads", THREAD_COUNT);
        log.info("Each thread performs {} increments", INCREMENTS_PER_THREAD);
        log.info("Expected value: {}", EXPECTED_VALUE);

        RaceConditionDemoResponse.CounterResult unsafeCounter = demonstrateRaceCondition();
        RaceConditionDemoResponse.CounterResult atomicCounter = demonstrateAtomicSolution();
        String takeaway = buildTakeaway(unsafeCounter, atomicCounter);

        log.info("Takeaway: {}", takeaway);

        return new RaceConditionDemoResponse(
                THREAD_COUNT,
                INCREMENTS_PER_THREAD,
                EXPECTED_VALUE,
                unsafeCounter,
                atomicCounter);
    }

    private RaceConditionDemoResponse.CounterResult buildCounterResult(String counterName, int actualValue) {
        int lostUpdates = EXPECTED_VALUE - actualValue;
        boolean matchesExpected = actualValue == EXPECTED_VALUE;
        String verdict = matchesExpected ? "SUCCESS" : "FAILURE";

        return new RaceConditionDemoResponse.CounterResult(
                counterName,
                actualValue,
                lostUpdates,
                matchesExpected,
                verdict);
    }

    private RaceConditionDemoResponse.CounterResult buildUnsafeCounterResult(int actualValue) {
        int lostUpdates = EXPECTED_VALUE - actualValue;
        boolean matchesExpected = actualValue == EXPECTED_VALUE;
        String verdict = matchesExpected
                ? "NO LOST UPDATES OBSERVED"
                : "RACE CONDITION OBSERVED";

        return new RaceConditionDemoResponse.CounterResult(
                "Unsafe counter",
                actualValue,
                lostUpdates,
                matchesExpected,
                verdict);
    }

    private String buildTakeaway(
            RaceConditionDemoResponse.CounterResult unsafeCounter,
            RaceConditionDemoResponse.CounterResult atomicCounter) {
        if (!unsafeCounter.isMatchesExpected() && atomicCounter.isMatchesExpected()) {
            return "Unsafe increments lose updates under contention, "
                    + "while atomic increments stay correct.";
        }

        return "Unsafe counter matched the expected value in this run, so increase contention or rerun the demo.";
    }

    private void executeUnsafeConcurrentIncrements(DemoCounters counters) throws InterruptedException {
        executeConcurrentIncrements(ignored -> counters.incrementUnsafe());
    }

    private void executeConcurrentIncrements(IntConsumer incrementAction) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> runWorker(incrementAction, readyGate, startGate));
            }

            releaseWorkers(readyGate, startGate);
            awaitTermination(executor);
        } catch (InterruptedException | RuntimeException exception) {
            executor.shutdownNow();
            throw exception;
        }
    }

    private void runWorker(IntConsumer incrementAction, CountDownLatch readyGate, CountDownLatch startGate) {
        readyGate.countDown();

        try {
            startGate.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }

        for (int iteration = 0; iteration < INCREMENTS_PER_THREAD; iteration++) {
            incrementAction.accept(iteration);
        }
    }

    private void releaseWorkers(CountDownLatch readyGate, CountDownLatch startGate) throws InterruptedException {
        if (!readyGate.await(WORKER_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            startGate.countDown();
            throw new IllegalStateException("Failed to prepare race-condition demo workers within 10 seconds");
        }
        startGate.countDown();
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

    private void logCounterResult(RaceConditionDemoResponse.CounterResult result) {
        log.info("{}:", result.getName());
        log.info("  Expected value: {}", EXPECTED_VALUE);
        log.info("  Actual value: {}", result.getActualValue());
        log.info("  Lost updates: {}", result.getLostUpdates());
        log.info("  Verdict: {}", result.getVerdict());
    }

    private static final class DemoCounters {

        private final AtomicInteger atomicCounter = new AtomicInteger();
        private int unsafeCounter;

        private void incrementUnsafe() {
            unsafeCounter++;
        }

        private int getUnsafeValue() {
            return unsafeCounter;
        }

        private void incrementAtomic() {
            atomicCounter.incrementAndGet();
        }

        private int getAtomicValue() {
            return atomicCounter.get();
        }
    }
}
