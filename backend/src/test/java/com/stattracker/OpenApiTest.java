package com.stattracker;

import com.stattracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The frontend's TypeScript types are generated from this document. */
class OpenApiTest extends IntegrationTest {

    @Test
    void describesTheApiForTheFrontend() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasKey("/api/players")))
                .andExpect(jsonPath("$.paths", hasKey("/api/players/{playerId}")))
                .andExpect(jsonPath("$.paths", hasKey("/api/players/{playerId}/games")))
                .andExpect(jsonPath("$.paths", hasKey("/api/players/{playerId}/season-averages")))
                .andExpect(jsonPath("$.paths", hasKey("/api/games/{gameId}/boxscore")))
                .andExpect(jsonPath("$.paths", hasKey("/api/data-status")))
                .andExpect(jsonPath("$.components.schemas.PlayerGame.properties.seasonType.enum",
                        containsInAnyOrder("REGULAR", "PLAY_IN", "PLAYOFFS")));
    }

    @Test
    void marksEveryPropertyRequiredAndOnlyNullableOnesNullable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.StatLine.required",
                        hasItems("minutes", "pts", "reb", "ast", "fg3m", "plusMinus")))
                .andExpect(jsonPath("$.components.schemas.StatLine.properties.pts.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.StatLine.properties.plusMinus.type",
                        containsInAnyOrder("integer", "null")))
                .andExpect(jsonPath("$.components.schemas.AverageLine.properties.fg3Pct.type",
                        containsInAnyOrder("number", "null")))
                .andExpect(jsonPath("$.components.schemas.PlayerProfile.properties.team.oneOf[1].type")
                        .value("null"))
                .andExpect(jsonPath("$.components.schemas.PlayerGame.properties.neutralSite.type").value("boolean"));
    }
}
