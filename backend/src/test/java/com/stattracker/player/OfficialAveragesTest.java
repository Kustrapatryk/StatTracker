package com.stattracker.player;

import java.util.List;

import com.stattracker.common.SeasonType;
import com.stattracker.ingestion.nba.NbaResultSet;
import com.stattracker.support.IntegrationTest;
import com.stattracker.support.NbaFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Our averages, computed from the imported game log, against NBA.com's official ones from
 * {@code playercareerstats} (Shai Gilgeous-Alexander 2025-26: 68 games, 31.1 PTS, 4.3 REB, 6.6 AST).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfficialAveragesTest extends IntegrationTest {

    private static final long SGA = 1628983L;

    @Autowired
    private SeasonAverageRepository seasonAverages;

    @BeforeAll
    void importData() {
        importFixtureSeason();
    }

    @ParameterizedTest
    @CsvSource({"REGULAR, SeasonTotalsRegularSeason", "PLAYOFFS, SeasonTotalsPostSeason"})
    void matchNbaComPerGameAverages(SeasonType seasonType, String officialResultSet) {
        Official nba = Official.of(NbaFixtures.load("playercareerstats-1628983.json").resultSet(officialResultSet),
                "2025-26");

        List<SeasonAverage> averages = seasonAverages.seasonAverages(SGA, seasonType);

        assertThat(averages).hasSize(1);
        AverageLine ours = averages.getFirst().stats();
        assertThat(ours.gamesPlayed()).isEqualTo((int) nba.value("GP"));
        assertThat(List.of(ours.pts(), ours.reb(), ours.ast(), ours.stl(), ours.blk(), ours.tov(), ours.pf(),
                ours.fgm(), ours.fga(), ours.fg3m(), ours.fg3a(), ours.ftm(), ours.fta(), ours.oreb(), ours.dreb()))
                .containsExactly(nba.value("PTS"), nba.value("REB"), nba.value("AST"), nba.value("STL"),
                        nba.value("BLK"), nba.value("TOV"), nba.value("PF"), nba.value("FGM"), nba.value("FGA"),
                        nba.value("FG3M"), nba.value("FG3A"), nba.value("FTM"), nba.value("FTA"), nba.value("OREB"),
                        nba.value("DREB"));
        assertThat(List.of(ours.fgPct(), ours.fg3Pct(), ours.ftPct()))
                .containsExactly(nba.value("FG_PCT"), nba.value("FG3_PCT"), nba.value("FT_PCT"));
        assertThat(ours.minutes()).as("game log minutes are whole numbers").isCloseTo(nba.value("MIN"), within(0.6));
    }

    private record Official(List<String> headers, List<Object> row) {

        static Official of(NbaResultSet resultSet, String seasonId) {
            int season = resultSet.headers().indexOf("SEASON_ID");
            List<Object> row = resultSet.rowSet().stream()
                    .filter(values -> seasonId.equals(values.get(season)))
                    .findFirst()
                    .orElseThrow();
            return new Official(resultSet.headers(), row);
        }

        double value(String column) {
            return ((Number) row.get(headers.indexOf(column))).doubleValue();
        }
    }
}
