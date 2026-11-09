package com.stattracker.ingestion.nba;

import java.time.Duration;

/** Keeps at least {@code interval} between the starts of consecutive requests. */
final class RequestThrottle {

    private final long intervalNanos;
    private long lastRequestAt;
    private boolean anyRequest;

    RequestThrottle(Duration interval) {
        this.intervalNanos = interval.toNanos();
    }

    synchronized void acquire() {
        if (anyRequest) {
            long waitNanos = lastRequestAt + intervalNanos - System.nanoTime();
            if (waitNanos > 0) {
                sleep(Duration.ofNanos(waitNanos));
            }
        }
        lastRequestAt = System.nanoTime();
        anyRequest = true;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the next NBA.com request slot", e);
        }
    }
}
