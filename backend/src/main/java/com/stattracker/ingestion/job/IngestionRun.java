package com.stattracker.ingestion.job;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * @param progress step being imported while the run is in progress, e.g. {@code 2019-20 PLAYOFFS}
 * @param error one line per failed step
 */
public record IngestionRun(
        long id,
        IngestionJob job,
        Map<String, String> params,
        RunStatus status,
        @Nullable String progress,
        Instant startedAt,
        @Nullable Instant finishedAt,
        int rowsUpserted,
        @Nullable String error) {
}
