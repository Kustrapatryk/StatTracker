package com.stattracker.ingestion.nba;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.stattracker.common.Season;
import com.stattracker.common.SeasonType;
import com.stattracker.ingestion.BoxStats;
import com.stattracker.ingestion.PlayerGameRecord;
import com.stattracker.ingestion.PlayerRecord;
import com.stattracker.ingestion.TeamGameRecord;
import com.stattracker.support.NbaFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NbaStatsProviderTest {

    private static final long OKC = 1610612760L;
    private static final long DEN = 1610612743L;
    private static final long LAC = 1610612746L;
    private static final long SGA = 1628983L;

    private final NbaStatsClient client = mock(NbaStatsClient.class);
    private final NbaStatsProvider provider = new NbaStatsProvider(client);

    @Test
    void mapsTeamGameLogAndRecognizesHomeTeamFromMatchup() {
        when(client.fetch(eq("leaguegamelog"), anyMap()))
                .thenReturn(NbaFixtures.load("leaguegamelog-T-2025-26-regular.json"));

        List<TeamGameRecord> lines = provider.fetchTeamGames(new Season(2025), SeasonType.REGULAR);

        assertThat(lines).hasSize(274);
        assertThat(lines).filteredOn(line -> line.gameId().equals("0022500933"))
                .extracting(TeamGameRecord::teamId, TeamGameRecord::teamName, TeamGameRecord::home,
                        TeamGameRecord::win, line -> line.stats().pts(), TeamGameRecord::minutes,
                        TeamGameRecord::plusMinus)
                .containsExactlyInAnyOrder(
                        tuple(OKC, "Oklahoma City Thunder", true, true, 129, 240, 3),
                        tuple(DEN, "Denver Nuggets", false, false, 126, 240, -3));
        assertThat(sentParams("leaguegamelog"))
                .containsEntry("PlayerOrTeam", "T")
                .containsEntry("Season", "2025-26")
                .containsEntry("SeasonType", "Regular Season")
                .containsEntry("LeagueID", "00");
    }

    @Test
    void mapsPlayerGameLog() {
        when(client.fetch(eq("leaguegamelog"), anyMap()))
                .thenReturn(NbaFixtures.load("leaguegamelog-P-2025-26-regular.json"));

        List<PlayerGameRecord> lines = provider.fetchPlayerGames(new Season(2025), SeasonType.REGULAR);

        assertThat(lines).hasSize(179);
        PlayerGameRecord sga = lines.stream()
                .filter(line -> line.playerId() == SGA && line.gameId().equals("0022500933"))
                .findFirst()
                .orElseThrow();
        assertThat(sga.gameDate()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(sga.playerName()).isEqualTo("Shai Gilgeous-Alexander");
        assertThat(sga.teamId()).isEqualTo(OKC);
        assertThat(sga.secondsPlayed()).isEqualTo(39 * 60);
        assertThat(sga.plusMinus()).isEqualTo(2);
        BoxStats stats = sga.stats();
        assertThat(List.of(stats.pts(), stats.reb(), stats.ast(), stats.fgm(), stats.fga(), stats.fg3m(),
                stats.fg3a(), stats.ftm(), stats.fta(), stats.stl(), stats.blk(), stats.tov(), stats.pf()))
                .containsExactly(35, 9, 15, 14, 21, 3, 7, 4, 4, 1, 1, 0, 3);
        assertThat(stats.oreb() + stats.dreb()).isEqualTo(stats.reb());
        assertThat(sentParams("leaguegamelog")).containsEntry("PlayerOrTeam", "P");
    }

    @Test
    void usesNbaNamesOfSeasonPhases() {
        when(client.fetch(eq("leaguegamelog"), anyMap())).thenReturn(NbaFixtures.load("leaguegamelog-empty.json"));

        provider.fetchTeamGames(new Season(2025), SeasonType.PLAY_IN);

        assertThat(sentParams("leaguegamelog")).containsEntry("SeasonType", "PlayIn");
    }

    @Test
    void dropsGamesOfOtherPhases() {
        when(client.fetch(eq("leaguegamelog"), anyMap()))
                .thenReturn(NbaFixtures.load("leaguegamelog-T-2025-26-playoffs.json"));

        assertThat(provider.fetchTeamGames(new Season(2025), SeasonType.REGULAR)).isEmpty();
        assertThat(provider.fetchTeamGames(new Season(2025), SeasonType.PLAYOFFS)).hasSize(66);
    }

    @Test
    void keepsTodaysFranchiseIdsForHistoricalTeams() {
        when(client.fetch(eq("leaguegamelog"), anyMap()))
                .thenReturn(NbaFixtures.load("leaguegamelog-P-2003-04-regular.json"));

        List<PlayerGameRecord> lines = provider.fetchPlayerGames(new Season(2003), SeasonType.REGULAR);

        assertThat(lines).hasSize(22).allSatisfy(line -> assertThat(line.gameId()).isEqualTo("0020301188"));
        assertThat(lines).extracting(PlayerGameRecord::teamId).as("Seattle SuperSonics = today's OKC")
                .containsOnly(OKC, LAC);
    }

    @Test
    void mapsPlayerIndex() {
        when(client.fetch(eq("playerindex"), anyMap())).thenReturn(NbaFixtures.load("playerindex-historical.json"));

        List<PlayerRecord> players = provider.fetchPlayers(new Season(2026));

        assertThat(players).hasSize(83);
        assertThat(players).filteredOn(player -> player.id() == SGA).singleElement().isEqualTo(new PlayerRecord(
                SGA, "Shai", "Gilgeous-Alexander", "G", "2", "6-6", 195, "Canada", 2018, OKC, true, 2018, 2026));
        PlayerRecord retired = players.stream().filter(player -> player.id() == 76001L).findFirst().orElseThrow();
        assertThat(retired.fullName()).isEqualTo("Alaa Abdelnaby");
        assertThat(retired.active()).isFalse();
        assertThat(retired.weightLbs()).isEqualTo(240);
        assertThat(sentParams("playerindex"))
                .containsEntry("Historical", "1")
                .containsEntry("Season", "2026-27");
    }

    @Test
    void recognizesHomeTeamOnlyInKnownMatchupFormats() {
        assertThat(NbaStatsProvider.isHome("OKC vs. DEN")).isTrue();
        assertThat(NbaStatsProvider.isHome("DEN @ OKC")).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> NbaStatsProvider.isHome("OKC - DEN"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> sentParams(String endpoint) {
        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(client, atLeastOnce()).fetch(eq(endpoint), params.capture());
        return params.getValue();
    }
}
