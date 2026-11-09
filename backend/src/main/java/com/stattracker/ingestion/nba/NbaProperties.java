package com.stattracker.ingestion.nba;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param minRequestInterval minimum time between two requests; NBA.com does not publish its rate limits
 */
@ConfigurationProperties("stattracker.nba")
public record NbaProperties(
        @DefaultValue("https://stats.nba.com/stats") String statsBaseUrl,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("60s") Duration readTimeout,
        @DefaultValue("1s") Duration minRequestInterval,
        @DefaultValue Retry retry) {

    public record Retry(
            @DefaultValue("3") int maxRetries,
            @DefaultValue("2s") Duration delay,
            @DefaultValue("2") double multiplier,
            @DefaultValue("30s") Duration maxDelay) {
    }
}
