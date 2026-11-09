package com.stattracker.player;

import java.time.LocalDate;
import java.util.List;

import com.stattracker.common.SeasonType;
import com.stattracker.common.StatLine;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PlayerGameRepository {

    private final JdbcClient jdbc;

    PlayerGameRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Most recent games first, across seasons and phases. */
    List<PlayerGame> recentGames(long playerId, int limit) {
        return jdbc.sql("""
                        SELECT pgs.game_id, g.game_date, g.season_id, g.season_type,
                               pgs.team_id, own.team_abbreviation, opp.team_id AS opponent_team_id,
                               opp.team_abbreviation AS opponent_abbreviation,
                               own.is_home, g.neutral_site, own.win, own.pts AS team_score, opp.pts AS opponent_score,
                               round(pgs.seconds_played / 60.0, 1) AS minutes,
                               pgs.pts, pgs.reb, pgs.ast, pgs.fgm, pgs.fga, pgs.fg3m, pgs.fg3a, pgs.ftm, pgs.fta,
                               pgs.oreb, pgs.dreb, pgs.stl, pgs.blk, pgs.tov, pgs.pf, pgs.plus_minus
                        FROM player_game_stats pgs
                        JOIN game g ON g.id = pgs.game_id
                        JOIN team_game_stats own ON own.game_id = pgs.game_id AND own.team_id = pgs.team_id
                        JOIN team_game_stats opp ON opp.game_id = pgs.game_id AND opp.team_id <> pgs.team_id
                        WHERE pgs.player_id = :playerId
                        ORDER BY g.game_date DESC, pgs.game_id DESC
                        LIMIT :limit""")
                .param("playerId", playerId)
                .param("limit", limit)
                .query((rs, rowNum) -> new PlayerGame(
                        rs.getString("game_id"),
                        rs.getObject("game_date", LocalDate.class),
                        rs.getString("season_id"),
                        SeasonType.valueOf(rs.getString("season_type")),
                        rs.getLong("team_id"),
                        rs.getString("team_abbreviation"),
                        rs.getLong("opponent_team_id"),
                        rs.getString("opponent_abbreviation"),
                        rs.getBoolean("is_home"),
                        rs.getBoolean("neutral_site"),
                        rs.getBoolean("win"),
                        rs.getInt("team_score"),
                        rs.getInt("opponent_score"),
                        StatLine.fromRow(rs)))
                .list();
    }
}
