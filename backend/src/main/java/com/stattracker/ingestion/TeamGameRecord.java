package com.stattracker.ingestion;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * @param teamAbbreviation abbreviation on the game day, e.g. SEA for today's OKC
 * @param teamName name on the game day, e.g. Seattle SuperSonics
 * @param home {@code false} for both teams at a neutral site, e.g. NBA Cup semifinals or games abroad
 */
public record TeamGameRecord(
        String gameId,
        LocalDate gameDate,
        long teamId,
        String teamAbbreviation,
        String teamName,
        boolean home,
        boolean win,
        int minutes,
        BoxStats stats,
        @Nullable Integer plusMinus) {
}
