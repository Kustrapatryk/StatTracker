package com.stattracker.common;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/** Box score line of a player or a team in one game. */
public record StatLine(
        double minutes,
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
        int pf,
        @Nullable Integer plusMinus) {

    /** Reads columns named like in {@code player_game_stats}; {@code minutes} must be selected explicitly. */
    public static StatLine fromRow(ResultSet rs) throws SQLException {
        Integer plusMinus = rs.getInt("plus_minus");
        if (rs.wasNull()) {
            plusMinus = null;
        }
        return new StatLine(
                rs.getDouble("minutes"),
                rs.getInt("pts"),
                rs.getInt("reb"),
                rs.getInt("ast"),
                rs.getInt("fgm"),
                rs.getInt("fga"),
                rs.getInt("fg3m"),
                rs.getInt("fg3a"),
                rs.getInt("ftm"),
                rs.getInt("fta"),
                rs.getInt("oreb"),
                rs.getInt("dreb"),
                rs.getInt("stl"),
                rs.getInt("blk"),
                rs.getInt("tov"),
                rs.getInt("pf"),
                plusMinus);
    }
}
