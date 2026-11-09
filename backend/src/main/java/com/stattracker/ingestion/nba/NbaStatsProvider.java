package com.stattracker.ingestion.nba;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;
import com.stattracker.ingestion.BoxStats;
import com.stattracker.ingestion.PlayerGameRecord;
import com.stattracker.ingestion.PlayerRecord;
import com.stattracker.ingestion.StatsProvider;
import com.stattracker.ingestion.TeamGameRecord;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** {@link StatsProvider} backed by the endpoints behind nba.com/stats. */
@Component
class NbaStatsProvider implements StatsProvider {

    private static final Logger log = LoggerFactory.getLogger(NbaStatsProvider.class);

    private final NbaStatsClient client;

    NbaStatsProvider(NbaStatsClient client) {
        this.client = client;
    }

    @Override
    public List<PlayerRecord> fetchPlayers(Season season) {
        Map<String, String> params = params(
                "College", "", "Country", "", "DraftPick", "", "DraftRound", "", "DraftYear", "", "Height", "",
                "Historical", "1", "LeagueID", "00", "Season", season.id(), "SeasonType", "Regular Season",
                "TeamID", "0", "Weight", "");
        return client.fetch("playerindex", params).resultSet("PlayerIndex").rows().stream()
                .map(NbaStatsProvider::toPlayer)
                .toList();
    }

    @Override
    public List<TeamGameRecord> fetchTeamGames(Season season, SeasonType type) {
        return gameLog("T", season, type).stream().map(NbaStatsProvider::toTeamGame).toList();
    }

    @Override
    public List<PlayerGameRecord> fetchPlayerGames(Season season, SeasonType type) {
        return gameLog("P", season, type).stream().map(NbaStatsProvider::toPlayerGame).toList();
    }

    /** {@code leaguegamelog} returns every game of a season phase in one response. */
    private List<NbaRow> gameLog(String playerOrTeam, Season season, SeasonType type) {
        Map<String, String> params = params(
                "Counter", "0", "DateFrom", "", "DateTo", "", "Direction", "DESC", "LeagueID", "00",
                "PlayerOrTeam", playerOrTeam, "Season", season.id(), "SeasonType", nbaSeasonType(type),
                "Sorter", "DATE");
        List<NbaRow> rows = client.fetch("leaguegamelog", params).resultSet("LeagueGameLog").rows();
        String prefix = gameIdPrefix(type);
        List<NbaRow> matching = rows.stream().filter(row -> row.string("GAME_ID").startsWith(prefix)).toList();
        if (matching.size() < rows.size()) {
            log.warn("{} {} {}: skipped {} rows with a game id outside the {}* range",
                    season, type, playerOrTeam, rows.size() - matching.size(), prefix);
        }
        return matching;
    }

    static String nbaSeasonType(SeasonType type) {
        return switch (type) {
            case REGULAR -> "Regular Season";
            case PLAY_IN -> "PlayIn";
            case PLAYOFFS -> "Playoffs";
        };
    }

    /** Game ids start with the phase: 001 preseason, 002 regular season, 004 playoffs, 005 play-in, 006 NBA Cup final. */
    static String gameIdPrefix(SeasonType type) {
        return switch (type) {
            case REGULAR -> "002";
            case PLAY_IN -> "005";
            case PLAYOFFS -> "004";
        };
    }

    static PlayerRecord toPlayer(NbaRow row) {
        Integer rosterStatus = row.optInteger("ROSTER_STATUS");
        Long teamId = row.optLong("TEAM_ID");
        return new PlayerRecord(
                row.longValue("PERSON_ID"),
                Objects.requireNonNullElse(row.optString("PLAYER_FIRST_NAME"), ""),
                Objects.requireNonNullElse(row.optString("PLAYER_LAST_NAME"), ""),
                row.optString("POSITION"),
                row.optString("JERSEY_NUMBER"),
                row.optString("HEIGHT"),
                parseIntOrNull(row.optString("WEIGHT")),
                row.optString("COUNTRY"),
                parseIntOrNull(row.optString("DRAFT_YEAR")),
                teamId == null || teamId == 0 ? null : teamId,
                rosterStatus != null && rosterStatus == 1,
                parseIntOrNull(row.optString("FROM_YEAR")),
                parseIntOrNull(row.optString("TO_YEAR")));
    }

    static TeamGameRecord toTeamGame(NbaRow row) {
        return new TeamGameRecord(
                row.string("GAME_ID"),
                LocalDate.parse(row.string("GAME_DATE")),
                row.longValue("TEAM_ID"),
                row.string("TEAM_ABBREVIATION"),
                row.string("TEAM_NAME"),
                isHome(row.string("MATCHUP")),
                "W".equals(row.optString("WL")),
                row.integer("MIN"),
                boxStats(row),
                row.optInteger("PLUS_MINUS"));
    }

    static PlayerGameRecord toPlayerGame(NbaRow row) {
        return new PlayerGameRecord(
                row.string("GAME_ID"),
                LocalDate.parse(row.string("GAME_DATE")),
                row.longValue("PLAYER_ID"),
                row.string("PLAYER_NAME"),
                row.longValue("TEAM_ID"),
                row.integer("MIN") * 60,
                boxStats(row),
                row.optInteger("PLUS_MINUS"));
    }

    /** {@code OKC vs. HOU} is the home team's row, {@code MIA @ LAC} the visitor's. */
    static boolean isHome(String matchup) {
        if (matchup.contains(" vs. ")) {
            return true;
        }
        if (matchup.contains(" @ ")) {
            return false;
        }
        throw new IllegalStateException("Unexpected MATCHUP: " + matchup);
    }

    private static BoxStats boxStats(NbaRow row) {
        return new BoxStats(
                row.integer("PTS"),
                row.integer("REB"),
                row.integer("AST"),
                row.integer("FGM"),
                row.integer("FGA"),
                row.integer("FG3M"),
                row.integer("FG3A"),
                row.integer("FTM"),
                row.integer("FTA"),
                row.integer("OREB"),
                row.integer("DREB"),
                row.integer("STL"),
                row.integer("BLK"),
                row.integer("TOV"),
                row.integer("PF"));
    }

    /** Profile fields are informational; an odd value must not fail the whole import. */
    private static @Nullable Integer parseIntOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, String> params(String... namesAndValues) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            params.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return params;
    }
}
