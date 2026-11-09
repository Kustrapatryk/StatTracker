package com.stattracker.ingestion.job;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "status")
class DataStatusController {

    private final JdbcClient jdbc;
    private final IngestionRunRepository runs;
    private final IngestionService ingestion;

    DataStatusController(JdbcClient jdbc, IngestionRunRepository runs, IngestionService ingestion) {
        this.jdbc = jdbc;
        this.runs = runs;
        this.ingestion = ingestion;
    }

    @GetMapping("/api/data-status")
    @Operation(summary = "How fresh the imported data is and whether an import is running")
    DataStatus status() {
        GameSummary games = jdbc.sql("""
                        SELECT (SELECT id FROM season WHERE is_current) AS latest_season,
                               max(game_date) AS last_game_date,
                               count(*) AS games
                        FROM game""")
                .query((rs, rowNum) -> new GameSummary(
                        rs.getString("latest_season"),
                        rs.getObject("last_game_date", LocalDate.class),
                        rs.getLong("games")))
                .single();
        return new DataStatus(
                games.latestSeason(),
                games.lastGameDate(),
                games.count(),
                ingestion.isRunning(),
                runs.lastSuccessAt().orElse(null),
                runs.recent(1).stream().findFirst().orElse(null));
    }

    private record GameSummary(@Nullable String latestSeason, @Nullable LocalDate lastGameDate, long count) {
    }
}
