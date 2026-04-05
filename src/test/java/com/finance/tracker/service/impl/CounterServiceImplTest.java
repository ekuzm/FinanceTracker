package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CounterServiceImplTest {

    private CounterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CounterServiceImpl();
    }

    @Test
    void incrementShouldIncreaseAtomicCounter() {
        service.increment();
        service.increment();

        assertEquals(2, service.getValue());
    }

    @Test
    void resetShouldClearAllCounters() {
        service.increment();
        service.incrementUnsafe();
        service.incrementSynchronized();

        service.reset();

        assertEquals(0, service.getValue());
        assertEquals(0, service.getUnsafeValue());
        assertEquals(0, service.getSynchronizedValue());
    }

    @Test
    void dedicatedCountersShouldTrackOwnValues() {
        service.incrementUnsafe();
        service.incrementUnsafe();
        service.incrementSynchronized();

        assertEquals(2, service.getUnsafeValue());
        assertEquals(1, service.getSynchronizedValue());
    }
}
