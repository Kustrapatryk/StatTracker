package com.stattracker.ingestion.nba;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class NbaClientConfig {

    /** Apache HttpClient transparently decompresses gzip: a season log is ~1 MB on the wire instead of 4.5 MB. */
    @Bean(destroyMethod = "close")
    CloseableHttpClient nbaHttpClient(NbaProperties properties) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(properties.connectTimeout()))
                .setSocketTimeout(Timeout.of(properties.readTimeout()))
                .build();
        return HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .build())
                // NbaStatsClient retries with backoff; immediate retries here would only add load.
                .disableAutomaticRetries()
                .build();
    }

    @Bean
    NbaStatsClient nbaStatsClient(
            RestClient.Builder restClientBuilder, CloseableHttpClient nbaHttpClient, NbaProperties properties) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(nbaHttpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return new NbaStatsClient(restClientBuilder.requestFactory(requestFactory), properties);
    }
}
