package com.stattracker.player;

import org.jspecify.annotations.Nullable;

/**
 * Per-game averages rounded like on NBA.com: one decimal, percentages as fractions with three decimals.
 *
 * @param fgPct {@code null} when there were no attempts
 */
public record AverageLine(
        int gamesPlayed,
        double minutes,
        double pts,
        double reb,
        double ast,
        double fgm,
        double fga,
        double fg3m,
        double fg3a,
        double ftm,
        double fta,
        double oreb,
        double dreb,
        double stl,
        double blk,
        double tov,
        double pf,
        @Nullable Double plusMinus,
        @Nullable Double fgPct,
        @Nullable Double fg3Pct,
        @Nullable Double ftPct) {
}
