package com.stattracker.player;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonTotalsTest {

    @Test
    void roundsHalfUpWithDecimalArithmetic() {
        AverageLine averages = totals(20, 3, 0, 0).averages();

        assertThat(averages.pts()).as("3 / 20 = 0.15").isEqualTo(0.2);
    }

    @Test
    void computesPercentagesFromSumsAndLeavesThemEmptyWithoutAttempts() {
        AverageLine averages = totals(2, 40, 15, 27).averages();

        assertThat(averages.fgPct()).isEqualTo(0.556);
        assertThat(averages.fg3Pct()).isNull();
        assertThat(averages.ftPct()).isNull();
    }

    @Test
    void combinesTeamsBySummingBeforeAveraging() {
        SeasonTotals total = totals(44, 1000, 0, 0).plus(totals(26, 400, 0, 0));

        assertThat(total.games()).isEqualTo(70);
        assertThat(total.averages().pts()).as("1400 / 70").isEqualTo(20.0);
    }

    @Test
    void averagesMinutesFromSeconds() {
        SeasonTotals totals = new SeasonTotals(3, 3 * 36 * 60 + 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -7, 3);

        assertThat(totals.averages().minutes()).isEqualTo(36.5);
        assertThat(totals.averages().plusMinus()).isEqualTo(-2.3);
    }

    private static SeasonTotals totals(int games, long pts, long fgm, long fga) {
        return new SeasonTotals(games, 0, pts, 0, 0, fgm, fga, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
