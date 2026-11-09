package com.stattracker.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.stattracker.common.SeasonType;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class SeasonAverageRepository {

    private final JdbcClient jdbc;

    SeasonAverageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Newest season first; within a season regular season, play-in, playoffs. */
    List<SeasonAverage> seasonAverages(long playerId, @Nullable SeasonType seasonType) {
        List<TeamTotals> rows = jdbc.sql("""
                        SELECT * FROM player_season_team_totals
                        WHERE player_id = :playerId
                          AND (CAST(:seasonType AS varchar) IS NULL OR season_type = :seasonType)
                        ORDER BY season_id DESC,
                                 CASE season_type WHEN 'REGULAR' THEN 0 WHEN 'PLAY_IN' THEN 1 ELSE 2 END,
                                 first_game_date""")
                .param("playerId", playerId)
                .param("seasonType", seasonType == null ? null : seasonType.name())
                .query((rs, rowNum) -> new TeamTotals(
                        rs.getString("season_id"),
                        SeasonType.valueOf(rs.getString("season_type")),
                        rs.getLong("team_id"),
                        rs.getString("team_abbreviation"),
                        SeasonTotals.fromRow(rs)))
                .list();

        Map<String, List<TeamTotals>> bySeason = new LinkedHashMap<>();
        rows.forEach(row -> bySeason.computeIfAbsent(row.seasonId() + "/" + row.seasonType(), key -> new ArrayList<>())
                .add(row));
        return bySeason.values().stream().map(SeasonAverageRepository::toSeasonAverage).toList();
    }

    private static SeasonAverage toSeasonAverage(List<TeamTotals> teams) {
        TeamTotals first = teams.getFirst();
        SeasonTotals total = teams.stream().map(TeamTotals::totals).reduce(SeasonTotals::plus).orElseThrow();
        List<SeasonAverage.TeamSeasonAverage> byTeam = teams.size() == 1
                ? List.of()
                : teams.stream()
                        .map(team -> new SeasonAverage.TeamSeasonAverage(
                                team.teamId(), team.teamAbbreviation(), team.totals().averages()))
                        .toList();
        return new SeasonAverage(
                first.seasonId(),
                first.seasonType(),
                teams.stream().map(TeamTotals::teamAbbreviation).toList(),
                total.averages(),
                byTeam);
    }

    private record TeamTotals(
            String seasonId, SeasonType seasonType, long teamId, String teamAbbreviation, SeasonTotals totals) {
    }
}
