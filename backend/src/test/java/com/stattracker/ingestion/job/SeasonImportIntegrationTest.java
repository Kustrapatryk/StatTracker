package com.stattracker.ingestion.job;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;
import com.stattracker.support.IntegrationTest;
import com.stattracker.support.NbaFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonImportIntegrationTest extends IntegrationTest {

    private static final Season SEASON = new Season(2025);
    private static final long OKC = 1610612760L;
    private static final long DEN = 1610612743L;
    private static final long SAS = 1610612759L;
    private static final long SGA = 1628983L;

    @Autowired
    private SeasonImporter importer;

    @BeforeEach
    void setUp() {
        resetDatabase();
        NbaFixtures.serve(nbaStatsClient);
    }

    @Test
    void importsGamesTeamLinesAndPlayerLinesOfEveryPhase() {
        importer.importPlayers(new Season(2026));
        importAllPhases();

        assertThat(count("game WHERE season_type = 'REGULAR'")).isEqualTo(137);
        assertThat(count("game WHERE season_type = 'PLAY_IN'")).isEqualTo(1);
        assertThat(count("game WHERE season_type = 'PLAYOFFS'")).isEqualTo(33);
        assertThat(count("team_game_stats")).isEqualTo(2 * 171);
        assertThat(count("player_game_stats")).isEqualTo(179 + 25 + 33);
        assertThat(count("player_game_stats WHERE player_id = " + SGA)).isEqualTo(68 + 15);

        Map<String, Object> game = jdbc.sql("SELECT * FROM game WHERE id = '0022500933'").query().singleRow();
        assertThat(game)
                .containsEntry("season_id", "2025-26")
                .containsEntry("home_team_id", OKC)
                .containsEntry("away_team_id", DEN)
                .containsEntry("home_score", 129)
                .containsEntry("away_score", 126);
        assertThat(jdbc.sql("SELECT game_date FROM game WHERE id = '0022500933'").query(LocalDate.class).single())
                .isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(jdbc.sql("SELECT * FROM game WHERE id = '0022501230'").query().singleRow())
                .as("NBA Cup semifinal in Las Vegas")
                .containsEntry("neutral_site", true)
                .containsEntry("home_team_id", SAS)
                .containsEntry("away_team_id", OKC)
                .containsEntry("home_score", 111)
                .containsEntry("away_score", 109);
        assertThat(jdbc.sql("SELECT seconds_played FROM player_game_stats WHERE game_id = '0022500933' AND player_id = :id")
                .param("id", SGA).query(Integer.class).single()).isEqualTo(39 * 60);
    }

    @Test
    void importingAgainUpdatesRowsInsteadOfDuplicatingThem() {
        importer.importPlayers(new Season(2026));
        int firstRun = importAllPhases();
        List<Long> before = tableCounts();

        int secondRun = importAllPhases();

        assertThat(tableCounts()).isEqualTo(before);
        assertThat(secondRun).isEqualTo(firstRun);
    }

    @Test
    void createsStubsForPlayersMissingFromTheIndexAndCompletesThemLater() {
        importer.importSeason(SEASON, SeasonType.REGULAR);

        assertThat(player(SGA))
                .containsEntry("first_name", "Shai")
                .containsEntry("last_name", "Gilgeous-Alexander")
                .containsEntry("search_name", "shai gilgeous-alexander")
                .containsEntry("is_active", false)
                .containsEntry("current_team_id", null);

        importer.importPlayers(new Season(2026));

        assertThat(player(SGA))
                .containsEntry("is_active", true)
                .containsEntry("current_team_id", OKC)
                .containsEntry("position", "G");
    }

    @Test
    void retiredPlayersHaveNoCurrentTeam() {
        importer.importPlayers(new Season(2026));

        assertThat(player(76001L))
                .containsEntry("full_name", "Alaa Abdelnaby")
                .containsEntry("is_active", false)
                .containsEntry("current_team_id", null);
        assertThat(player(203999L)).containsEntry("search_name", "nikola jokic");
    }

    @Test
    void phaseWithoutGamesWritesNothing() {
        assertThat(importer.importSeason(new Season(2026), SeasonType.PLAYOFFS)).isZero();

        assertThat(count("season")).isZero();
        assertThat(count("game")).isZero();
    }

    @Test
    void latestImportedSeasonIsCurrent() {
        jdbc.sql("INSERT INTO season (id, start_year, is_current) VALUES ('2003-04', 2003, TRUE)").update();

        importer.importSeason(SEASON, SeasonType.REGULAR);

        assertThat(jdbc.sql("SELECT id FROM season WHERE is_current").query(String.class).list())
                .containsExactly("2025-26");
    }

    private int importAllPhases() {
        int rows = 0;
        for (SeasonType type : SeasonType.values()) {
            rows += importer.importSeason(SEASON, type);
        }
        return rows;
    }

    private List<Long> tableCounts() {
        return List.of(count("season"), count("player"), count("game"), count("team_game_stats"),
                count("player_game_stats"));
    }

    private Map<String, Object> player(long id) {
        return jdbc.sql("SELECT * FROM player WHERE id = :id").param("id", id).query().singleRow();
    }
}
