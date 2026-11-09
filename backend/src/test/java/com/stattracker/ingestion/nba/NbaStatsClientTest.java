package com.stattracker.ingestion.nba;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.stattracker.support.NbaFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NbaStatsClientTest {

    private static final String URL =
            "https://stats.nba.com/stats/leaguegamelog?PlayerOrTeam=T&Season=2025-26&SeasonType=Regular%20Season&DateFrom=";

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final NbaStatsClient client = new NbaStatsClient(restClientBuilder, new NbaProperties(
            "https://stats.nba.com/stats", Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ZERO,
            new NbaProperties.Retry(2, Duration.ofMillis(1), 1.0, Duration.ofMillis(1))));

    @Test
    void sendsBrowserHeadersAndParsesResultSets() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Referer", "https://www.nba.com/"))
                .andExpect(header("Origin", "https://www.nba.com"))
                .andExpect(header("Sec-Fetch-Mode", "cors"))
                .andExpect(header("Sec-Fetch-Site", "same-site"))
                .andRespond(withSuccess(NbaFixtures.text("leaguegamelog-T-2025-26-playin.json"), MediaType.APPLICATION_JSON));

        NbaResponse response = client.fetch("leaguegamelog", params());

        server.verify();
        NbaResultSet log = response.resultSet("LeagueGameLog");
        assertThat(log.rows()).hasSize(2);
        assertThat(log.rows().getFirst().string("MATCHUP")).isEqualTo("PHX vs. GSW");
    }

    @Test
    void retriesTimeoutsAndServerErrors() {
        server.expect(requestTo(URL)).andRespond(withServiceUnavailable());
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        server.expect(requestTo(URL))
                .andRespond(withSuccess(NbaFixtures.text("leaguegamelog-empty.json"), MediaType.APPLICATION_JSON));

        NbaResponse response = client.fetch("leaguegamelog", params());

        server.verify();
        assertThat(response.resultSet("LeagueGameLog").rows()).isEmpty();
    }

    @Test
    void givesUpAfterTheLastRetry() {
        server.expect(requestTo(URL)).andRespond(withServiceUnavailable());
        server.expect(requestTo(URL)).andRespond(withServiceUnavailable());
        server.expect(requestTo(URL)).andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> client.fetch("leaguegamelog", params()))
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
        server.verify();
    }

    @Test
    void doesNotRetryRejectedRequests() {
        server.expect(requestTo(URL)).andRespond(withBadRequest());

        assertThatThrownBy(() -> client.fetch("leaguegamelog", params()))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
        server.verify();
    }

    private static Map<String, String> params() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("PlayerOrTeam", "T");
        params.put("Season", "2025-26");
        params.put("SeasonType", "Regular Season");
        params.put("DateFrom", "");
        return params;
    }
}
