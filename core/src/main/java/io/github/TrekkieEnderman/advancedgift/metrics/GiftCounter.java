package io.github.TrekkieEnderman.advancedgift.metrics;

import java.util.concurrent.atomic.AtomicInteger;

public class GiftCounter {
    private final AtomicInteger counter = new AtomicInteger();

    public void increment() {
        counter.incrementAndGet();
    }

    public int collect() {
        return counter.getAndSet(0);
    }
}
