package com.stattracker.ingestion;

import org.jspecify.annotations.Nullable;

/**
 * @param teamId last known team; for retired players it can be a franchise that no longer exists
 */
public record PlayerRecord(
        long id,
        String firstName,
        String lastName,
        @Nullable String position,
        @Nullable String jerseyNumber,
        @Nullable String height,
        @Nullable Integer weightLbs,
        @Nullable String country,
        @Nullable Integer draftYear,
        @Nullable Long teamId,
        boolean active,
        @Nullable Integer fromYear,
        @Nullable Integer toYear) {

    public String fullName() {
        return (firstName + " " + lastName).strip();
    }
}
