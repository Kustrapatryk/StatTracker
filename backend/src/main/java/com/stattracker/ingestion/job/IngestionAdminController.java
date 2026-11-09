package com.stattracker.ingestion.job;

import java.util.List;

import com.stattracker.common.Season;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ingestion")
@Tag(name = "admin", description = "Importing data from NBA.com")
class IngestionAdminController {

    private final IngestionService ingestion;
    private final IngestionRunRepository runs;
    private final IngestionProperties properties;

    IngestionAdminController(IngestionService ingestion, IngestionRunRepository runs, IngestionProperties properties) {
        this.ingestion = ingestion;
        this.runs = runs;
        this.properties = properties;
    }

    @PostMapping("/backfill")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Import players and all seasons from the given one to the current one (runs in background)")
    IngestionRun backfill(
            @Parameter(description = "First season, defaults to stattracker.ingestion.backfill-from", example = "2003-04")
            @RequestParam(required = false) @Nullable String from) {
        return ingestion.startBackfill(Season.parse(from != null ? from : properties.backfillFrom()));
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Re-import players and the current season (runs in background)")
    IngestionRun sync() {
        return ingestion.startDailySync();
    }

    @GetMapping("/runs")
    @Operation(summary = "Import history, newest first")
    List<IngestionRun> runs(@RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {
        return runs.recent(limit);
    }
}
