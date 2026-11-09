package com.stattracker.game;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
@Tag(name = "games")
class GameController {

    private final BoxScoreRepository boxScores;

    GameController(BoxScoreRepository boxScores) {
        this.boxScores = boxScores;
    }

    @GetMapping("/{gameId}/boxscore")
    @Operation(summary = "Box score of both teams with team totals")
    BoxScore boxScore(@Parameter(example = "0022500933") @PathVariable @Pattern(regexp = "\\d{10}") String gameId) {
        return boxScores.find(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game " + gameId + " not found"));
    }
}
