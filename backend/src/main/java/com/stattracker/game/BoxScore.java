package com.stattracker.game;

import java.time.LocalDate;
import java.util.List;

import com.stattracker.common.SeasonType;
import com.stattracker.common.StatLine;
import org.jspecify.annotations.Nullable;

/**
 * @param neutralSite home and away are only nominal, e.g. NBA Cup semifinals in Las Vegas
 */
public record BoxScore(
        String gameId,
        LocalDate gameDate,
        String seasonId,
        SeasonType seasonType,
        boolean neutralSite,
        TeamBoxScore home,
        TeamBoxScore away) {

    /**
     * @param abbreviation abbreviation and name as on the game day
     * @param players sorted by minutes played
     */
    public record TeamBoxScore(
            long teamId,
            String abbreviation,
            String name,
            int score,
            boolean win,
            StatLine totals,
            List<PlayerLine> players) {
    }

    /**
     * @param starter {@code null} while the game is known only from the game log, which has no starters
     */
    public record PlayerLine(
            long playerId,
            String name,
            @Nullable String position,
            @Nullable Boolean starter,
            StatLine stats) {
    }
}
