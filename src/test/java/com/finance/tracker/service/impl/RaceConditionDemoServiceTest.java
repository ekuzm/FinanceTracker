package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.dto.response.RaceConditionDemoResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class RaceConditionDemoServiceTest {

    private final RaceConditionDemoService service = new RaceConditionDemoService();

    @Test
    void runAllDemosShouldReturnStructuredAndExplicitResults(CapturedOutput output) throws InterruptedException {
        RaceConditionDemoResponse response = service.runAllDemos();

        String logs = output.toString();

        assertTrue(logs.contains("Starting race condition demonstration with 50 threads"));
        assertTrue(logs.contains("Expected value: 50000"));
        assertTrue(logs.contains("Unsafe counter:"));
        assertTrue(logs.contains("Atomic counter:"));
        assertTrue(logs.contains("Takeaway:"));

        assertEquals(50, response.getThreadCount());
        assertEquals(1000, response.getIncrementsPerThread());
        assertEquals(50_000, response.getExpectedValue());
        assertEquals("Unsafe counter", response.getUnsafeCounter().getName());
        assertTrue(response.getUnsafeCounter().getActualValue() <= 50_000);
        assertEquals(
                50_000 - response.getUnsafeCounter().getActualValue(),
                response.getUnsafeCounter().getLostUpdates());
        assertTrue(response.getAtomicCounter().isMatchesExpected());
    }

    @Test
    void demonstrateRaceConditionShouldReturnConsistentUnsafeResult() throws InterruptedException {
        RaceConditionDemoResponse.CounterResult result = service.demonstrateRaceCondition();

        assertEquals("Unsafe counter", result.getName());
        assertTrue(result.getActualValue() <= 50_000);
        assertEquals(50_000 - result.getActualValue(), result.getLostUpdates());
        assertEquals(
                result.isMatchesExpected() ? "NO LOST UPDATES OBSERVED" : "RACE CONDITION OBSERVED",
                result.getVerdict());
    }

    @Test
    void demonstrateAtomicCounterShouldReachExpectedValue() throws InterruptedException {
        RaceConditionDemoResponse.CounterResult atomicResult = service.demonstrateAtomicSolution();

        assertEquals(50_000, atomicResult.getActualValue());
        assertEquals(0, atomicResult.getLostUpdates());
        assertTrue(atomicResult.isMatchesExpected());
        assertEquals("SUCCESS", atomicResult.getVerdict());
    }

    @Test
    void awaitTerminationShouldThrowWhenExecutorDoesNotFinishInTime() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(1, TimeUnit.MINUTES)).thenReturn(false);

        Method awaitTerminationMethod = privateMethod("awaitTermination", ExecutorService.class);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> awaitTerminationMethod.invoke(service, executor));

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(
                "Failed to complete race-condition demo within 1 minute",
                exception.getCause().getMessage());
        verify(executor).shutdown();
        verify(executor).shutdownNow();
    }

    @Test
    void awaitTerminationShouldShutdownAndRethrowInterruptedException() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        InterruptedException interruptedException = new InterruptedException("interrupted");
        when(executor.awaitTermination(1, TimeUnit.MINUTES)).thenThrow(interruptedException);

        Method awaitTerminationMethod = privateMethod("awaitTermination", ExecutorService.class);

        try {
            InvocationTargetException exception = assertThrows(
                    InvocationTargetException.class,
                    () -> awaitTerminationMethod.invoke(service, executor));

            assertEquals(interruptedException, exception.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(executor).shutdown();
            verify(executor).shutdownNow();
        } finally {
            Thread.interrupted();
        }
    }

    private Method privateMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = RaceConditionDemoService.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}
