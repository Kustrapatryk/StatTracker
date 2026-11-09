package com.stattracker.player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/** Sums of a player's stats over a set of games; averages are derived from the sums, never averaged again. */
record SeasonTotals(
        int games,
        long seconds,
        long pts,
        long reb,
        long ast,
        long fgm,
        long fga,
        long fg3m,
        long fg3a,
        long ftm,
        long fta,
        long oreb,
        long dreb,
        long stl,
        long blk,
        long tov,
        long pf,
        long plusMinus,
        int plusMinusGames) {

    static SeasonTotals fromRow(ResultSet rs) throws SQLException {
        return new SeasonTotals(
                rs.getInt("games_played"),
                rs.getLong("seconds_played"),
                rs.getLong("pts"),
                rs.getLong("reb"),
                rs.getLong("ast"),
                rs.getLong("fgm"),
                rs.getLong("fga"),
                rs.getLong("fg3m"),
                rs.getLong("fg3a"),
                rs.getLong("ftm"),
                rs.getLong("fta"),
                rs.getLong("oreb"),
                rs.getLong("dreb"),
                rs.getLong("stl"),
                rs.getLong("blk"),
                rs.getLong("tov"),
                rs.getLong("pf"),
                rs.getLong("plus_minus"),
                rs.getInt("plus_minus_games"));
    }

    SeasonTotals plus(SeasonTotals other) {
        return new SeasonTotals(
                games + other.games,
                seconds + other.seconds,
                pts + other.pts,
                reb + other.reb,
                ast + other.ast,
                fgm + other.fgm,
                fga + other.fga,
                fg3m + other.fg3m,
                fg3a + other.fg3a,
                ftm + other.ftm,
                fta + other.fta,
                oreb + other.oreb,
                dreb + other.dreb,
                stl + other.stl,
                blk + other.blk,
                tov + other.tov,
                pf + other.pf,
                plusMinus + other.plusMinus,
                plusMinusGames + other.plusMinusGames);
    }

    AverageLine averages() {
        return new AverageLine(
                games,
                divide(seconds, 60L * games, 1),
                perGame(pts),
                perGame(reb),
                perGame(ast),
                perGame(fgm),
                perGame(fga),
                perGame(fg3m),
                perGame(fg3a),
                perGame(ftm),
                perGame(fta),
                perGame(oreb),
                perGame(dreb),
                perGame(stl),
                perGame(blk),
                perGame(tov),
                perGame(pf),
                plusMinusGames == 0 ? null : divide(plusMinus, plusMinusGames, 1),
                percentage(fgm, fga),
                percentage(fg3m, fg3a),
                percentage(ftm, fta));
    }

    private double perGame(long total) {
        return divide(total, games, 1);
    }

    private static @Nullable Double percentage(long made, long attempted) {
        return attempted == 0 ? null : divide(made, attempted, 3);
    }

    /** Decimal arithmetic, so 0.15 rounds to 0.2 as on NBA.com and not to 0.1 as binary doubles would. */
    private static double divide(long dividend, long divisor, int scale) {
        return BigDecimal.valueOf(dividend).divide(BigDecimal.valueOf(divisor), scale, RoundingMode.HALF_UP).doubleValue();
    }
}
