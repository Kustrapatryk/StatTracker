package com.stattracker.ingestion;

import java.util.List;

import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;

/** Source of NBA data. NBA.com is the only implementation; a licensed provider could replace it. */
public interface StatsProvider {

    /** Every player the source knows, retired ones included, with roster status as of {@code season}. */
    List<PlayerRecord> fetchPlayers(Season season);

    /** One row per team per completed game. */
    List<TeamGameRecord> fetchTeamGames(Season season, SeasonType type);

    /** One row per player per completed game, only for players who got on the floor. */
    List<PlayerGameRecord> fetchPlayerGames(Season season, SeasonType type);
}
