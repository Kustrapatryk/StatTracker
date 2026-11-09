package com.stattracker.player;

import org.jspecify.annotations.Nullable;

/**
 * @param teamId current team, {@code null} for free agents and retired players
 */
public record PlayerSearchResult(
        long id,
        String fullName,
        @Nullable Long teamId,
        @Nullable String teamAbbreviation,
        @Nullable String position,
        boolean active,
        @Nullable Integer fromYear,
        @Nullable Integer toYear) {
}
