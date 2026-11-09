package com.stattracker.game;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.stattracker.common.SeasonType;
import com.stattracker.common.StatLine;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class BoxScoreRepository {

    private final JdbcClient jdbc;

    BoxScoreRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<BoxScore> find(String gameId) {
        Optional<GameHeader> header = jdbc.sql("""
                        SELECT id, game_date, season_id, season_type, neutral_site, home_team_id, away_team_id
                        FROM game WHERE id = :gameId""")
                .param("gameId", gameId)
                .query((rs, rowNum) -> new GameHeader(
                        rs.getString("id"),
                        rs.getObject("game_date", LocalDate.class),
                        rs.getString("season_id"),
                        SeasonType.valueOf(rs.getString("season_type")),
                        rs.getBoolean("neutral_site"),
                        rs.getLong("home_team_id"),
                        rs.getLong("away_team_id")))
                .optional();
        return header.map(this::load);
    }

    private BoxScore load(GameHeader game) {
        Map<Long, List<BoxScore.PlayerLine>> playersByTeam = jdbc.sql("""
                        SELECT pgs.team_id, pgs.player_id, p.full_name, p.position, pgs.is_starter,
                               round(pgs.seconds_played / 60.0, 1) AS minutes,
                               pgs.pts, pgs.reb, pgs.ast, pgs.fgm, pgs.fga, pgs.fg3m, pgs.fg3a, pgs.ftm, pgs.fta,
                               pgs.oreb, pgs.dreb, pgs.stl, pgs.blk, pgs.tov, pgs.pf, pgs.plus_minus
                        FROM player_game_stats pgs
                        JOIN player p ON p.id = pgs.player_id
                        WHERE pgs.game_id = :gameId
                        ORDER BY pgs.seconds_played DESC, pgs.pts DESC, p.full_name""")
                .param("gameId", game.id())
                .query((rs, rowNum) -> Map.entry(rs.getLong("team_id"), new BoxScore.PlayerLine(
                        rs.getLong("player_id"),
                        rs.getString("full_name"),
                        rs.getString("position"),
                        (Boolean) rs.getObject("is_starter"),
                        StatLine.fromRow(rs))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Map<Long, BoxScore.TeamBoxScore> teams = jdbc.sql("""
                        SELECT team_id, team_abbreviation, team_name, win, minutes,
                               pts, reb, ast, fgm, fga, fg3m, fg3a, ftm, fta,
                               oreb, dreb, stl, blk, tov, pf, plus_minus
                        FROM team_game_stats
                        WHERE game_id = :gameId""")
                .param("gameId", game.id())
                .query((rs, rowNum) -> new BoxScore.TeamBoxScore(
                        rs.getLong("team_id"),
                        rs.getString("team_abbreviation"),
                        rs.getString("team_name"),
                        rs.getInt("pts"),
                        rs.getBoolean("win"),
                        StatLine.fromRow(rs),
                        playersByTeam.getOrDefault(rs.getLong("team_id"), List.of())))
                .list()
                .stream()
                .collect(Collectors.toMap(BoxScore.TeamBoxScore::teamId, team -> team));

        return new BoxScore(game.id(), game.date(), game.seasonId(), game.seasonType(), game.neutralSite(),
                teams.get(game.homeTeamId()), teams.get(game.awayTeamId()));
    }

    private record GameHeader(
            String id,
            LocalDate date,
            String seasonId,
            SeasonType seasonType,
            boolean neutralSite,
            long homeTeamId,
            long awayTeamId) {
    }
}
