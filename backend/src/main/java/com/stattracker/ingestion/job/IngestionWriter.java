package com.stattracker.ingestion.job;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.stattracker.common.SearchNames;
import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;
import com.stattracker.ingestion.BoxStats;
import com.stattracker.ingestion.PlayerGameRecord;
import com.stattracker.ingestion.PlayerRecord;
import com.stattracker.ingestion.TeamGameRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/** Idempotent batch upserts: importing the same data again updates rows instead of duplicating them. */
@Repository
class IngestionWriter {

    private static final int BATCH_SIZE = 1000;

    private static final List<String> STAT_COLUMNS = List.of(
            "pts", "reb", "ast", "fgm", "fga", "fg3m", "fg3a", "ftm", "fta", "oreb", "dreb", "stl", "blk", "tov", "pf");

    private static final String UPSERT_PLAYER = upsert("player", List.of("id"), List.of(
            "first_name", "last_name", "full_name", "search_name", "position", "jersey_number", "height", "weight_lbs",
            "country", "draft_year", "current_team_id", "is_active", "from_year", "to_year"), true);

    private static final String INSERT_PLAYER_STUB = """
            INSERT INTO player (id, first_name, last_name, full_name, search_name, is_active)
            VALUES (?, ?, ?, ?, ?, FALSE)
            ON CONFLICT (id) DO NOTHING""";

    private static final String UPSERT_GAME = upsert("game", List.of("id"), List.of(
            "season_id", "season_type", "game_date", "home_team_id", "away_team_id", "home_score", "away_score",
            "neutral_site", "status"), true);

    private static final String UPSERT_TEAM_GAME_STATS = upsert("team_game_stats", List.of("game_id", "team_id"),
            concat(List.of("team_abbreviation", "team_name", "is_home", "win", "minutes"), STAT_COLUMNS,
                    List.of("plus_minus")), false);

    private static final String UPSERT_PLAYER_GAME_STATS = upsert("player_game_stats", List.of("game_id", "player_id"),
            concat(List.of("team_id", "seconds_played"), STAT_COLUMNS, List.of("plus_minus", "source")), true);

    private final JdbcTemplate jdbc;

    IngestionWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void upsertSeason(Season season) {
        jdbc.update("INSERT INTO season (id, start_year) VALUES (?, ?) ON CONFLICT (id) DO NOTHING",
                season.id(), season.startYear());
        jdbc.update("UPDATE season SET is_current = (start_year = (SELECT max(start_year) FROM season))");
    }

    void upsertPlayers(List<PlayerRecord> players) {
        Set<Long> teamIds = new HashSet<>(jdbc.queryForList("SELECT id FROM team", Long.class));
        batch(UPSERT_PLAYER, players, (ps, player) -> {
            // Retired players point at their last team, which may be a defunct franchise.
            Long currentTeamId = player.active() && player.teamId() != null && teamIds.contains(player.teamId())
                    ? player.teamId()
                    : null;
            ps.setLong(1, player.id());
            ps.setString(2, player.firstName());
            ps.setString(3, player.lastName());
            ps.setString(4, player.fullName());
            ps.setString(5, SearchNames.normalize(player.fullName()));
            ps.setString(6, player.position());
            ps.setString(7, player.jerseyNumber());
            ps.setString(8, player.height());
            setInteger(ps, 9, player.weightLbs());
            ps.setString(10, player.country());
            setInteger(ps, 11, player.draftYear());
            ps.setObject(12, currentTeamId, Types.BIGINT);
            ps.setBoolean(13, player.active());
            setInteger(ps, 14, player.fromYear());
            setInteger(ps, 15, player.toYear());
        });
    }

    /**
     * Game logs can mention a player the player index does not know yet (e.g. a debut on the day of the import).
     * A stub keeps the foreign key valid; the next player import fills in the details.
     */
    void insertMissingPlayers(List<PlayerGameRecord> lines) {
        Map<Long, String> names = new LinkedHashMap<>();
        lines.forEach(line -> names.putIfAbsent(line.playerId(), line.playerName()));
        batch(INSERT_PLAYER_STUB, names.entrySet(), (ps, player) -> {
            String fullName = player.getValue().strip();
            int space = fullName.indexOf(' ');
            ps.setLong(1, player.getKey());
            ps.setString(2, space < 0 ? "" : fullName.substring(0, space));
            ps.setString(3, space < 0 ? fullName : fullName.substring(space + 1));
            ps.setString(4, fullName);
            ps.setString(5, SearchNames.normalize(fullName));
        });
    }

    void upsertGames(Season season, SeasonType type, List<GameRow> games) {
        batch(UPSERT_GAME, games, (ps, game) -> {
            ps.setString(1, game.id());
            ps.setString(2, season.id());
            ps.setString(3, type.name());
            ps.setObject(4, game.date());
            ps.setLong(5, game.homeTeamId());
            ps.setLong(6, game.awayTeamId());
            ps.setInt(7, game.homeScore());
            ps.setInt(8, game.awayScore());
            ps.setBoolean(9, game.neutralSite());
            ps.setString(10, "FINAL");
        });
    }

    void upsertTeamGameStats(List<TeamGameRecord> lines) {
        batch(UPSERT_TEAM_GAME_STATS, lines, (ps, line) -> {
            ps.setString(1, line.gameId());
            ps.setLong(2, line.teamId());
            ps.setString(3, line.teamAbbreviation());
            ps.setString(4, line.teamName());
            ps.setBoolean(5, line.home());
            ps.setBoolean(6, line.win());
            ps.setInt(7, line.minutes());
            int next = setStats(ps, 8, line.stats());
            setInteger(ps, next, line.plusMinus());
        });
    }

    void upsertPlayerGameStats(List<PlayerGameRecord> lines) {
        batch(UPSERT_PLAYER_GAME_STATS, lines, (ps, line) -> {
            ps.setString(1, line.gameId());
            ps.setLong(2, line.playerId());
            ps.setLong(3, line.teamId());
            ps.setInt(4, line.secondsPlayed());
            int next = setStats(ps, 5, line.stats());
            setInteger(ps, next, line.plusMinus());
            ps.setString(next + 1, "NBA_GAMELOG");
        });
    }

    private <T> void batch(String sql, Collection<T> items, ParameterizedPreparedStatementSetter<T> setter) {
        if (!items.isEmpty()) {
            jdbc.batchUpdate(sql, items, BATCH_SIZE, setter);
        }
    }

    /** Sets the {@link #STAT_COLUMNS} starting at {@code index}; returns the next free parameter index. */
    private static int setStats(PreparedStatement ps, int index, BoxStats stats) throws SQLException {
        int[] values = {stats.pts(), stats.reb(), stats.ast(), stats.fgm(), stats.fga(), stats.fg3m(), stats.fg3a(),
                stats.ftm(), stats.fta(), stats.oreb(), stats.dreb(), stats.stl(), stats.blk(), stats.tov(), stats.pf()};
        for (int value : values) {
            ps.setInt(index++, value);
        }
        return index;
    }

    private static void setInteger(PreparedStatement ps, int index, @Nullable Integer value) throws SQLException {
        ps.setObject(index, value, Types.INTEGER);
    }

    private static String upsert(String table, List<String> keys, List<String> columns, boolean touchUpdatedAt) {
        List<String> all = concat(keys, columns);
        List<String> updates = new ArrayList<>(columns.stream().map(column -> column + " = EXCLUDED." + column).toList());
        if (touchUpdatedAt) {
            updates.add("updated_at = now()");
        }
        return "INSERT INTO " + table + " (" + String.join(", ", all) + ")"
                + " VALUES (" + all.stream().map(column -> "?").collect(Collectors.joining(", ")) + ")"
                + " ON CONFLICT (" + String.join(", ", keys) + ") DO UPDATE SET " + String.join(", ", updates);
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        return Stream.of(lists).flatMap(List::stream).toList();
    }
}
