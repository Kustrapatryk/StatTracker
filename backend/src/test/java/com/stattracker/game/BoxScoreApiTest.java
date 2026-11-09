package com.stattracker.game;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.stattracker.support.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoxScoreApiTest extends IntegrationTest {

    @BeforeAll
    void importData() {
        importFixtureSeason();
    }

    @Test
    void returnsBothTeamsWithPlayersSortedByMinutesAndTeamTotals() throws Exception {
        String json = mockMvc.perform(get("/api/games/0022500933/boxscore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameDate").value("2026-03-09"))
                .andExpect(jsonPath("$.seasonId").value("2025-26"))
                .andExpect(jsonPath("$.seasonType").value("REGULAR"))
                .andExpect(jsonPath("$.neutralSite").value(false))
                .andExpect(jsonPath("$.home.abbreviation").value("OKC"))
                .andExpect(jsonPath("$.home.name").value("Oklahoma City Thunder"))
                .andExpect(jsonPath("$.home.score").value(129))
                .andExpect(jsonPath("$.home.win").value(true))
                .andExpect(jsonPath("$.home.totals.pts").value(129))
                .andExpect(jsonPath("$.home.totals.minutes").value(240.0))
                .andExpect(jsonPath("$.home.players[0].name").value("Jaylin Williams"))
                .andExpect(jsonPath("$.home.players[0].stats.minutes").value(40.0))
                .andExpect(jsonPath("$.home.players[?(@.playerId == 1628983)].stats.ast").value(15))
                .andExpect(jsonPath("$.away.abbreviation").value("DEN"))
                .andExpect(jsonPath("$.away.score").value(126))
                .andExpect(jsonPath("$.away.win").value(false))
                .andExpect(jsonPath("$.away.players[?(@.playerId == 203999)].name").value("Nikola Jokić"))
                .andReturn().getResponse().getContentAsString();

        List<Integer> homePoints = JsonPath.read(json, "$.home.players[*].stats.pts");
        List<Integer> awayPoints = JsonPath.read(json, "$.away.players[*].stats.pts");
        assertThat(homePoints.stream().mapToInt(Integer::intValue).sum()).isEqualTo(129);
        assertThat(awayPoints.stream().mapToInt(Integer::intValue).sum()).isEqualTo(126);
        assertThat(homePoints.size() + awayPoints.size()).isEqualTo(19);
    }

    @Test
    void flagsNeutralSiteGames() throws Exception {
        mockMvc.perform(get("/api/games/0022501230/boxscore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.neutralSite").value(true))
                .andExpect(jsonPath("$.home.abbreviation").value("SAS"))
                .andExpect(jsonPath("$.away.abbreviation").value("OKC"))
                .andExpect(jsonPath("$.away.score").value(109));
    }

    @Test
    void unknownGameIsNotFound() throws Exception {
        mockMvc.perform(get("/api/games/0029999999/boxscore"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Game 0029999999 not found"));
    }

    @Test
    void rejectsMalformedGameIds() throws Exception {
        mockMvc.perform(get("/api/games/abc/boxscore")).andExpect(status().isBadRequest());
    }
}
