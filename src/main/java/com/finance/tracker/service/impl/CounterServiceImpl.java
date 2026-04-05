package com.finance.tracker.service.impl;

import com.finance.tracker.service.CounterService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class CounterServiceImpl implements CounterService {

    private final AtomicInteger atomicCounter = new AtomicInteger(0);
    private int unsafeCounter = 0;
    private int synchronizedCounter = 0;

    @Override
    public void increment() {
        atomicCounter.incrementAndGet();
    }

    @Override
    public int getValue() {
        return atomicCounter.get();
    }

    @Override
    public void reset() {
        atomicCounter.set(0);
        unsafeCounter = 0;
        synchronizedCounter = 0;
    }

    public void incrementUnsafe() {
        unsafeCounter++;
    }

    public int getUnsafeValue() {
        return unsafeCounter;
    }

    public synchronized void incrementSynchronized() {
        synchronizedCounter++;
    }

    public int getSynchronizedValue() {
        return synchronizedCounter;
    }
}
