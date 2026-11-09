package com.stattracker.player;

import java.time.LocalDate;

import com.stattracker.common.SeasonType;
import com.stattracker.common.StatLine;

/**
 * One game of a player with every stat, so the client can switch stat and line without another request.
 *
 * @param gameDate US date of the game
 * @param teamAbbreviation abbreviations as on the game day, e.g. SEA in 2007-08
 * @param home {@code false} for both teams at a neutral site
 */
public record PlayerGame(
        String gameId,
        LocalDate gameDate,
        String seasonId,
        SeasonType seasonType,
        long teamId,
        String teamAbbreviation,
        long opponentTeamId,
        String opponentAbbreviation,
        boolean home,
        boolean neutralSite,
        boolean win,
        int teamScore,
        int opponentScore,
        StatLine stats) {
}
