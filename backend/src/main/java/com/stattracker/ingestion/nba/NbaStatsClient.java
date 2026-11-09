package com.stattracker.ingestion.nba;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP access to stats.nba.com: browser headers, one request at a time, retries with backoff. */
public class NbaStatsClient {

    private static final Logger log = LoggerFactory.getLogger(NbaStatsClient.class);

    /**
     * stats.nba.com answers only requests that look like they come from www.nba.com. Without the
     * {@code Sec-Fetch-*} headers it never responds, so the request hangs until the read timeout.
     */
    static final Map<String, String> BROWSER_HEADERS = browserHeaders();

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final RequestThrottle throttle;

    public NbaStatsClient(RestClient.Builder restClientBuilder, NbaProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.statsBaseUrl())
                .defaultHeaders(headers -> BROWSER_HEADERS.forEach(headers::set))
                .build();
        NbaProperties.Retry retry = properties.retry();
        this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(retry.maxRetries())
                .delay(retry.delay())
                .multiplier(retry.multiplier())
                .maxDelay(retry.maxDelay())
                .predicate(NbaStatsClient::isTransient)
                .build());
        this.throttle = new RequestThrottle(properties.minRequestInterval());
    }

    /** Calls {@code stats.nba.com/stats/{endpoint}}; after the last failed attempt rethrows its exception. */
    public NbaResponse fetch(String endpoint, Map<String, String> params) {
        return retryTemplate.invoke(() -> {
            throttle.acquire();
            long start = System.nanoTime();
            try {
                NbaResponse response = restClient.get()
                        .uri(uri -> {
                            uri.pathSegment(endpoint);
                            params.forEach((name, value) -> uri.queryParam(name, value));
                            return uri.build();
                        })
                        .retrieve()
                        .body(NbaResponse.class);
                if (response == null) {
                    throw new IllegalStateException("Empty response from " + endpoint);
                }
                log.debug("{} {} answered in {} ms", endpoint, params, elapsedMillis(start));
                return response;
            } catch (RuntimeException e) {
                log.warn("{} {} failed after {} ms: {}", endpoint, params, elapsedMillis(start), e.getMessage());
                throw e;
            }
        });
    }

    static boolean isTransient(Throwable error) {
        return error instanceof ResourceAccessException
                || error instanceof HttpServerErrorException
                || error instanceof HttpClientErrorException.TooManyRequests
                || (error instanceof RestClientException && error.getCause() instanceof IOException);
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static Map<String, String> browserHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Referer", "https://www.nba.com/");
        headers.put("Origin", "https://www.nba.com");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-site");
        return Collections.unmodifiableMap(headers);
    }
}
