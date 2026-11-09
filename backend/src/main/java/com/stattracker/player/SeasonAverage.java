package com.stattracker.player;

import java.util.List;

import com.stattracker.common.SeasonType;

/**
 * @param teams abbreviations in the order the player played for them, e.g. [LAC, CLE] after a trade
 * @param stats averages over all teams
 * @param byTeam one entry per team when the player played for more than one, otherwise empty
 */
public record SeasonAverage(
        String seasonId,
        SeasonType seasonType,
        List<String> teams,
        AverageLine stats,
        List<TeamSeasonAverage> byTeam) {

    public record TeamSeasonAverage(long teamId, String teamAbbreviation, AverageLine stats) {
    }
}
