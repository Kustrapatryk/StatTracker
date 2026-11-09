package com.stattracker.player;

import com.stattracker.support.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerApiTest extends IntegrationTest {

    @BeforeAll
    void importData() {
        importFixtureSeason();
    }

    @ParameterizedTest
    @ValueSource(strings = {"gilg", "Gilgeous", "shai g", "alexander", "gilgeus"})
    void findsPlayersByAnyPartOfTheNameAndToleratesTypos(String query) throws Exception {
        mockMvc.perform(get("/api/players").param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1628983))
                .andExpect(jsonPath("$[0].fullName").value("Shai Gilgeous-Alexander"))
                .andExpect(jsonPath("$[0].teamAbbreviation").value("OKC"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jokic", "Jokić", "JOKIC", "nikola jok"})
    void ignoresDiacriticsAndCase(String query) throws Exception {
        mockMvc.perform(get("/api/players").param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Nikola Jokić"));
    }

    @Test
    void treatsLikeWildcardsLiterally() throws Exception {
        mockMvc.perform(get("/api/players").param("query", "__"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void rejectsTooShortQueries() throws Exception {
        mockMvc.perform(get("/api/players").param("query", "g"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void returnsProfileWithCurrentTeam() throws Exception {
        mockMvc.perform(get("/api/players/1628983"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Shai Gilgeous-Alexander"))
                .andExpect(jsonPath("$.team.abbreviation").value("OKC"))
                .andExpect(jsonPath("$.team.city").value("Oklahoma City"))
                .andExpect(jsonPath("$.team.name").value("Thunder"))
                .andExpect(jsonPath("$.position").value("G"))
                .andExpect(jsonPath("$.jerseyNumber").value("2"))
                .andExpect(jsonPath("$.height").value("6-6"))
                .andExpect(jsonPath("$.weightLbs").value(195))
                .andExpect(jsonPath("$.draftYear").value(2018))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void retiredPlayerHasNoTeam() throws Exception {
        mockMvc.perform(get("/api/players/76001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team").value((Object) null))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void unknownPlayerIsNotFound() throws Exception {
        mockMvc.perform(get("/api/players/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Player 1 not found"));
        mockMvc.perform(get("/api/players/1/games")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/players/1/season-averages")).andExpect(status().isNotFound());
    }

    @Test
    void listsGamesNewestFirstAcrossPhases() throws Exception {
        mockMvc.perform(get("/api/players/1628983/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(68 + 15)))
                .andExpect(jsonPath("$[0].gameId").value("0042500317"))
                .andExpect(jsonPath("$[0].gameDate").value("2026-05-30"))
                .andExpect(jsonPath("$[0].seasonId").value("2025-26"))
                .andExpect(jsonPath("$[0].seasonType").value("PLAYOFFS"))
                .andExpect(jsonPath("$[0].home").value(true))
                .andExpect(jsonPath("$[0].opponentAbbreviation").value("SAS"))
                .andExpect(jsonPath("$[0].stats.pts").value(35))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].home").value(true))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].neutralSite").value(false))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].teamAbbreviation").value("OKC"))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].opponentAbbreviation").value("DEN"))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].opponentTeamId").value(1610612743))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].teamScore").value(129))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].opponentScore").value(126))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].win").value(true))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].stats.minutes").value(39.0))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].stats.ast").value(15))
                .andExpect(jsonPath("$[?(@.gameId == '0022500933')].stats.plusMinus").value(2));
    }

    @Test
    void marksNeutralSiteGames() throws Exception {
        mockMvc.perform(get("/api/players/1628983/games"))
                .andExpect(jsonPath("$[?(@.gameId == '0022501230')].neutralSite").value(true))
                .andExpect(jsonPath("$[?(@.gameId == '0022501230')].home").value(false))
                .andExpect(jsonPath("$[?(@.gameId == '0022501230')].opponentAbbreviation").value("SAS"))
                .andExpect(jsonPath("$[?(@.gameId == '0022501230')].win").value(false));
    }

    @Test
    void limitsGames() throws Exception {
        mockMvc.perform(get("/api/players/1628983/games").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        mockMvc.perform(get("/api/players/1628983/games").param("limit", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/players/1628983/games").param("limit", "501")).andExpect(status().isBadRequest());
    }

    @Test
    void splitsSeasonAveragesByTeamAfterATrade() throws Exception {
        mockMvc.perform(get("/api/players/201935/season-averages").param("seasonType", "REGULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].seasonId").value("2025-26"))
                .andExpect(jsonPath("$[0].teams", contains("LAC", "CLE")))
                .andExpect(jsonPath("$[0].stats.gamesPlayed").value(70))
                .andExpect(jsonPath("$[0].byTeam[0].teamAbbreviation").value("LAC"))
                .andExpect(jsonPath("$[0].byTeam[0].stats.gamesPlayed").value(44))
                .andExpect(jsonPath("$[0].byTeam[1].teamAbbreviation").value("CLE"))
                .andExpect(jsonPath("$[0].byTeam[1].stats.gamesPlayed").value(26));
    }

    @Test
    void listsEveryPhaseOfASeasonWhenNoPhaseIsGiven() throws Exception {
        mockMvc.perform(get("/api/players/1628983/season-averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].seasonType", contains("REGULAR", "PLAYOFFS")))
                .andExpect(jsonPath("$[0].teams", contains("OKC")))
                .andExpect(jsonPath("$[0].byTeam", empty()))
                .andExpect(jsonPath("$[0].stats.pts").value(31.1));
    }

    @Test
    void rejectsUnknownSeasonType() throws Exception {
        mockMvc.perform(get("/api/players/1628983/season-averages").param("seasonType", "PRESEASON"))
                .andExpect(status().isBadRequest());
    }
}
