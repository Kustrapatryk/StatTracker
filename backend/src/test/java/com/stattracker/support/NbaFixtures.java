package com.stattracker.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.stattracker.ingestion.nba.NbaResponse;
import com.stattracker.ingestion.nba.NbaStatsClient;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Real stats.nba.com responses trimmed to a few players, stored in {@code src/test/resources/nba-fixtures}:
 * all 2025-26 games of Shai Gilgeous-Alexander and James Harden (traded LAC to CLE), two complete regular season
 * games, one play-in game and one 2003-04 game of the Seattle SuperSonics.
 */
public final class NbaFixtures {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private NbaFixtures() {
    }

    public static String text(String name) {
        try (InputStream in = NbaFixtures.class.getResourceAsStream("/nba-fixtures/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("No fixture " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static NbaResponse load(String name) {
        return JSON.readValue(text(name), NbaResponse.class);
    }

    public static boolean exists(String name) {
        return NbaFixtures.class.getResource("/nba-fixtures/" + name) != null;
    }

    /** Answers every request like stats.nba.com would, from fixtures; seasons without fixtures have no games. */
    public static void serve(NbaStatsClient client) {
        when(client.fetch(eq("playerindex"), anyMap())).thenAnswer(invocation -> load("playerindex-historical.json"));
        when(client.fetch(eq("leaguegamelog"), anyMap())).thenAnswer(invocation -> {
            Map<String, String> params = invocation.getArgument(1);
            String name = "leaguegamelog-%s-%s-%s.json".formatted(
                    params.get("PlayerOrTeam"), params.get("Season"), phase(params.get("SeasonType")));
            return load(exists(name) ? name : "leaguegamelog-empty.json");
        });
    }

    private static String phase(String nbaSeasonType) {
        return switch (nbaSeasonType) {
            case "Regular Season" -> "regular";
            case "PlayIn" -> "playin";
            case "Playoffs" -> "playoffs";
            default -> throw new IllegalArgumentException(nbaSeasonType);
        };
    }
}
