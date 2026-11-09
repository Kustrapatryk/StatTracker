package com.stattracker.ingestion.job;

public enum IngestionJob {
    /** Player index and every season from a given one up to the current one. */
    BACKFILL,
    /** Player index and every phase of the current season. */
    DAILY_SYNC
}
