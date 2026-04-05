package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
