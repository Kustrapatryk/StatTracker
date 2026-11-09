package com.stattracker.ingestion.job;

public enum RunStatus {
    RUNNING,
    SUCCESS,
    /** At least one step failed; the other steps were still imported. */
    FAILED
}
