package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void runAllDemosShouldLogEachCounterScenario(CapturedOutput output) throws InterruptedException {
        service.runAllDemos();

        String logs = output.toString();

        assertTrue(logs.contains("Starting race condition demonstration with 50 threads"));
        assertTrue(logs.contains("Expected value: 50000"));
        assertTrue(logs.contains("Unsafe counter:"));
        assertTrue(logs.contains("Synchronized counter:"));
        assertTrue(logs.contains("Atomic counter:"));
        assertTrue(logs.contains("Result: SUCCESS"));
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

    @Test
    void logResultsShouldCoverUnsafeCounterOutcomes(CapturedOutput output) throws Exception {
        Method logResultsMethod = privateMethod("logResults", String.class, int.class);

        logResultsMethod.invoke(service, "Unsafe counter", 50_000);
        logResultsMethod.invoke(service, "Unsafe counter", 49_999);

        String logs = output.toString();

        assertTrue(logs.contains("Lost updates: 0"));
        assertTrue(logs.contains("Race condition: ABSENT"));
        assertTrue(logs.contains("Lost updates: 1"));
        assertTrue(logs.contains("Race condition: PRESENT"));
    }

    @Test
    void logResultsShouldCoverSafeCounterOutcomes(CapturedOutput output) throws Exception {
        Method logResultsMethod = privateMethod("logResults", String.class, int.class);

        logResultsMethod.invoke(service, "Atomic counter", 50_000);
        logResultsMethod.invoke(service, "Atomic counter", 49_999);

        String logs = output.toString();

        assertTrue(logs.contains("Result: SUCCESS"));
        assertTrue(logs.contains("Result: FAILURE"));
    }

    private Method privateMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = RaceConditionDemoService.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}
