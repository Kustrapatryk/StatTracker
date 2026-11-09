package com.stattracker.player;

import java.util.List;

import com.stattracker.common.SeasonType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/players")
@Tag(name = "players")
class PlayerController {

    private final PlayerRepository players;
    private final PlayerGameRepository games;
    private final SeasonAverageRepository seasonAverages;

    PlayerController(PlayerRepository players, PlayerGameRepository games, SeasonAverageRepository seasonAverages) {
        this.players = players;
        this.games = games;
        this.seasonAverages = seasonAverages;
    }

    @GetMapping
    @Operation(summary = "Search players by name; diacritics and small typos are ignored")
    List<PlayerSearchResult> search(
            @Parameter(example = "gilgeous") @RequestParam @Size(min = 2, max = 60) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return players.search(query, limit);
    }

    @GetMapping("/{playerId}")
    @Operation(summary = "Player profile")
    PlayerProfile profile(@Parameter(example = "1628983") @PathVariable long playerId) {
        return players.find(playerId).orElseThrow(() -> playerNotFound(playerId));
    }

    @GetMapping("/{playerId}/games")
    @Operation(summary = "Games played, most recent first, with the full stat line")
    List<PlayerGame> games(
            @Parameter(example = "1628983") @PathVariable long playerId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        requirePlayer(playerId);
        return games.recentGames(playerId, limit);
    }

    @GetMapping("/{playerId}/season-averages")
    @Operation(summary = "Per-game averages of every season, split by team after a trade")
    List<SeasonAverage> seasonAverages(
            @Parameter(example = "1628983") @PathVariable long playerId,
            @Parameter(description = "All phases when omitted") @RequestParam(required = false) @Nullable SeasonType seasonType) {
        requirePlayer(playerId);
        return seasonAverages.seasonAverages(playerId, seasonType);
    }

    private void requirePlayer(long playerId) {
        if (!players.exists(playerId)) {
            throw playerNotFound(playerId);
        }
    }

    private static ResponseStatusException playerNotFound(long playerId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Player " + playerId + " not found");
    }
}
