package com.stattracker.ingestion;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record PlayerGameRecord(
        String gameId,
        LocalDate gameDate,
        long playerId,
        String playerName,
        long teamId,
        int secondsPlayed,
        BoxStats stats,
        @Nullable Integer plusMinus) {
}
