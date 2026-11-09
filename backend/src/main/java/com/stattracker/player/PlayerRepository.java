package com.stattracker.player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.stattracker.common.SearchNames;
import com.stattracker.common.TeamRef;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PlayerRepository {

    /**
     * Substring or trigram word similarity (catches typos such as "jokcic"). Players who match at the start of a
     * word come first, then active players, then the closest matches. Retired players without any imported game
     * are left out because there is nothing to show for them.
     */
    private static final String SEARCH = """
            SELECT p.id, p.full_name, p.current_team_id, t.abbreviation AS team_abbreviation, p.position,
                   p.is_active, p.from_year, p.to_year
            FROM player p
            LEFT JOIN team t ON t.id = p.current_team_id
            WHERE (p.search_name LIKE :contains OR :query <% p.search_name)
              AND (p.is_active OR EXISTS (SELECT 1 FROM player_game_stats s WHERE s.player_id = p.id))
            ORDER BY (p.search_name LIKE :prefix OR p.search_name LIKE :wordPrefix) DESC,
                     p.is_active DESC,
                     word_similarity(:query, p.search_name) DESC,
                     p.to_year DESC NULLS LAST,
                     p.full_name
            LIMIT :limit""";

    private final JdbcClient jdbc;

    PlayerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<PlayerSearchResult> search(String query, int limit) {
        String normalized = SearchNames.normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }
        String pattern = escapeLike(normalized);
        return jdbc.sql(SEARCH)
                .param("query", normalized)
                .param("contains", "%" + pattern + "%")
                .param("prefix", pattern + "%")
                .param("wordPrefix", "% " + pattern + "%")
                .param("limit", limit)
                .query((rs, rowNum) -> new PlayerSearchResult(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        nullableLong(rs, "current_team_id"),
                        rs.getString("team_abbreviation"),
                        rs.getString("position"),
                        rs.getBoolean("is_active"),
                        nullableInt(rs, "from_year"),
                        nullableInt(rs, "to_year")))
                .list();
    }

    Optional<PlayerProfile> find(long id) {
        return jdbc.sql("""
                        SELECT p.*, t.abbreviation AS team_abbreviation, t.city AS team_city, t.name AS team_name
                        FROM player p
                        LEFT JOIN team t ON t.id = p.current_team_id
                        WHERE p.id = :id""")
                .param("id", id)
                .query((rs, rowNum) -> new PlayerProfile(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        team(rs),
                        rs.getString("position"),
                        rs.getString("jersey_number"),
                        rs.getString("height"),
                        nullableInt(rs, "weight_lbs"),
                        rs.getString("country"),
                        nullableInt(rs, "draft_year"),
                        rs.getBoolean("is_active"),
                        nullableInt(rs, "from_year"),
                        nullableInt(rs, "to_year")))
                .optional();
    }

    boolean exists(long id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM player WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single();
    }

    private static @Nullable TeamRef team(ResultSet rs) throws SQLException {
        Long teamId = nullableLong(rs, "current_team_id");
        return teamId == null ? null : new TeamRef(
                teamId, rs.getString("team_abbreviation"), rs.getString("team_city"), rs.getString("team_name"));
    }

    /** Backslash is the default LIKE escape character in PostgreSQL. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static @Nullable Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
