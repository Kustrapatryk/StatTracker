package com.stattracker.ingestion.nba;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestThrottleTest {

    @Test
    void spacesOutConsecutiveRequests() {
        RequestThrottle throttle = new RequestThrottle(Duration.ofMillis(150));

        long start = System.nanoTime();
        throttle.acquire();
        throttle.acquire();
        throttle.acquire();

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(300));
    }

    @Test
    void doesNotDelayTheFirstRequest() {
        RequestThrottle throttle = new RequestThrottle(Duration.ofSeconds(10));

        long start = System.nanoTime();
        throttle.acquire();

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
    }
}
