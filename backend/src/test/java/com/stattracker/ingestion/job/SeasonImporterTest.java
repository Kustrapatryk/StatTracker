package com.stattracker.ingestion.job;

import java.time.LocalDate;
import java.util.List;

import com.stattracker.ingestion.BoxStats;
import com.stattracker.ingestion.TeamGameRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonImporterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 9);
    private static final long OKC = 1610612760L;
    private static final long DEN = 1610612743L;
    private static final long SAS = 1610612759L;

    @Test
    void pairsHomeAndAwayLinesIntoGames() {
        List<GameRow> games = SeasonImporter.pairTeams(List.of(
                line("0022500933", DEN, false, 126),
                line("0022500933", OKC, true, 129)));

        assertThat(games).containsExactly(new GameRow("0022500933", DATE, OKC, DEN, 129, 126, false));
    }

    @Test
    void makesTheLowerTeamIdTheNominalHomeTeamAtNeutralSites() {
        List<GameRow> games = SeasonImporter.pairTeams(List.of(
                line("0022501230", OKC, false, 109),
                line("0022501230", SAS, false, 111)));

        assertThat(games).containsExactly(new GameRow("0022501230", DATE, SAS, OKC, 111, 109, true));
    }

    @Test
    void skipsGamesWithoutLinesOfTwoTeams() {
        List<GameRow> games = SeasonImporter.pairTeams(List.of(
                line("0022500001", OKC, true, 110),
                line("0022500002", DEN, true, 100),
                line("0022500002", DEN, false, 100)));

        assertThat(games).isEmpty();
    }

    private static TeamGameRecord line(String gameId, long teamId, boolean home, int pts) {
        BoxStats stats = new BoxStats(pts, 40, 25, 40, 85, 12, 35, 20, 25, 10, 30, 8, 5, 12, 20);
        return new TeamGameRecord(gameId, DATE, teamId, "ABC", "Team " + teamId, home, home, 240, stats, 0);
    }
}
