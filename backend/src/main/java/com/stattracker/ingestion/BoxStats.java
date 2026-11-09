package com.stattracker.ingestion;

/** Counting stats of a player or a team in one game. */
public record BoxStats(
        int pts,
        int reb,
        int ast,
        int fgm,
        int fga,
        int fg3m,
        int fg3a,
        int ftm,
        int fta,
        int oreb,
        int dreb,
        int stl,
        int blk,
        int tov,
        int pf) {
}
