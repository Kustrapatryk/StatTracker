package com.stattracker.ingestion.job;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * @param lastGameDate US date of the most recent imported game
 * @param lastSuccessAt when the data was last refreshed successfully
 */
public record DataStatus(
        @Nullable String latestSeason,
        @Nullable LocalDate lastGameDate,
        long games,
        boolean importRunning,
        @Nullable Instant lastSuccessAt,
        @Nullable IngestionRun lastRun) {
}
