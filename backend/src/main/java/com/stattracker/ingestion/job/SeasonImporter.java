package com.stattracker.ingestion.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;
import com.stattracker.ingestion.PlayerGameRecord;
import com.stattracker.ingestion.PlayerRecord;
import com.stattracker.ingestion.StatsProvider;
import com.stattracker.ingestion.TeamGameRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Imports one step of a job: the player index or one phase of one season. */
@Component
class SeasonImporter {

    private static final Logger log = LoggerFactory.getLogger(SeasonImporter.class);

    private final StatsProvider provider;
    private final IngestionWriter writer;
    private final TransactionTemplate transaction;

    SeasonImporter(StatsProvider provider, IngestionWriter writer, TransactionTemplate transaction) {
        this.provider = provider;
        this.writer = writer;
        this.transaction = transaction;
    }

    /** @return number of rows written */
    int importPlayers(Season season) {
        List<PlayerRecord> players = provider.fetchPlayers(season);
        transaction.executeWithoutResult(status -> writer.upsertPlayers(players));
        log.info("Players: {} upserted", players.size());
        return players.size();
    }

    /** Games, team lines and player lines of one season phase, written in a single transaction. */
    int importSeason(Season season, SeasonType type) {
        List<TeamGameRecord> teamLines = provider.fetchTeamGames(season, type);
        if (teamLines.isEmpty()) {
            log.info("{} {}: no games", season, type);
            return 0;
        }
        List<GameRow> games = pairTeams(teamLines);
        Set<String> gameIds = games.stream().map(GameRow::id).collect(Collectors.toSet());
        List<TeamGameRecord> teamRows = teamLines.stream().filter(line -> gameIds.contains(line.gameId())).toList();
        List<PlayerGameRecord> playerLines = provider.fetchPlayerGames(season, type);
        List<PlayerGameRecord> playerRows = playerLines.stream()
                .filter(line -> gameIds.contains(line.gameId()))
                .toList();
        if (playerRows.size() < playerLines.size()) {
            log.warn("{} {}: skipped {} player lines of games without both team lines",
                    season, type, playerLines.size() - playerRows.size());
        }

        transaction.executeWithoutResult(status -> {
            writer.upsertSeason(season);
            writer.upsertGames(season, type, games);
            writer.upsertTeamGameStats(teamRows);
            writer.insertMissingPlayers(playerRows);
            writer.upsertPlayerGameStats(playerRows);
        });
        log.info("{} {}: {} games, {} player lines", season, type, games.size(), playerRows.size());
        return games.size() + teamRows.size() + playerRows.size();
    }

    /**
     * A game has one line per team; together they give the matchup and the score. At neutral sites both teams
     * are listed as visitors, so the team with the lower id becomes the nominal home team, which stays stable
     * across re-imports.
     */
    static List<GameRow> pairTeams(List<TeamGameRecord> teamLines) {
        Map<String, List<TeamGameRecord>> byGame = teamLines.stream()
                .collect(Collectors.groupingBy(TeamGameRecord::gameId, LinkedHashMap::new, Collectors.toList()));
        List<GameRow> games = new ArrayList<>(byGame.size());
        byGame.forEach((gameId, lines) -> {
            if (lines.size() != 2 || lines.get(0).teamId() == lines.get(1).teamId()) {
                log.warn("Game {} skipped: expected lines of two teams, got {}", gameId, lines);
                return;
            }
            boolean neutralSite = lines.get(0).home() == lines.get(1).home();
            TeamGameRecord home = neutralSite
                    ? lines.stream().min(Comparator.comparingLong(TeamGameRecord::teamId)).orElseThrow()
                    : lines.stream().filter(TeamGameRecord::home).findFirst().orElseThrow();
            TeamGameRecord away = lines.get(0) == home ? lines.get(1) : lines.get(0);
            games.add(new GameRow(gameId, home.gameDate(), home.teamId(), away.teamId(),
                    home.stats().pts(), away.stats().pts(), neutralSite));
        });
        return games;
    }
}
