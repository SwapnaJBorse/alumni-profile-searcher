package com.alumnisearch.scraper;

import com.alumnisearch.config.PhantomBusterProperties;
import com.alumnisearch.exception.ScraperException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PhantomBusterClientTest {

    private static final String BASE = "https://api.phantombuster.com/api/v2";
    private static final ProfileSearchCriteria CRITERIA = new ProfileSearchCriteria("XYZ", "Engineer", 2020);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private PhantomBusterClient client(String apiKey, String agentId, long timeoutMs) {
        PhantomBusterProperties properties = new PhantomBusterProperties(
                BASE, apiKey, agentId, "cookie", false, 1, timeoutMs, 10);
        return new PhantomBusterClient(builder, properties, new PhantomBusterResultMapper(), objectMapper);
    }

    @Test
    void launchesPollsUntilFinishedAndMapsResults() throws Exception {
        String rows = objectMapper.writeValueAsString(List.of(
                Map.of("fullName", "John Doe", "headline", "Software Engineer at XYZ",
                        "location", "Pune", "profileUrl", "https://www.linkedin.com/in/john")));
        String resultBody = objectMapper.writeValueAsString(Map.of("resultObject", rows));

        server.expect(requestTo(BASE + "/agents/launch"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Phantombuster-Key", "key"))
                .andRespond(withSuccess("{\"containerId\":\"c1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/containers/fetch-output?id=c1"))
                .andRespond(withSuccess("{\"status\":\"running\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/containers/fetch-output?id=c1"))
                .andRespond(withSuccess("{\"status\":\"finished\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/containers/fetch-result-object?id=c1"))
                .andRespond(withSuccess(resultBody, MediaType.APPLICATION_JSON));

        List<ScrapedProfile> result = client("key", "agent", 5_000).search(CRITERIA);

        assertThat(result).singleElement().satisfies(p -> {
            assertThat(p.name()).isEqualTo("John Doe");
            assertThat(p.currentRole()).isEqualTo("Software Engineer");
        });
        server.verify();
    }

    @Test
    void failsFastWhenNotConfigured() {
        assertThatThrownBy(() -> client("", "agent", 1_000).search(CRITERIA))
                .isInstanceOfSatisfying(ScraperException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void mapsHttpErrorsToBadGateway() {
        server.expect(requestTo(BASE + "/agents/launch")).andRespond(withServerError());

        assertThatThrownBy(() -> client("key", "agent", 1_000).search(CRITERIA))
                .isInstanceOfSatisfying(ScraperException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void timesOutWhenAgentNeverFinishes() {
        server.expect(requestTo(BASE + "/agents/launch"))
                .andRespond(withSuccess("{\"containerId\":\"c1\"}", MediaType.APPLICATION_JSON));
        server.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(),
                        requestTo(BASE + "/containers/fetch-output?id=c1"))
                .andRespond(withSuccess("{\"status\":\"running\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client("key", "agent", 30).search(CRITERIA))
                .isInstanceOfSatisfying(ScraperException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void failsWhenFinishedWithoutResultObject() {
        server.expect(requestTo(BASE + "/agents/launch"))
                .andRespond(withSuccess("{\"containerId\":\"c1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/containers/fetch-output?id=c1"))
                .andRespond(withSuccess("{\"status\":\"finished\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/containers/fetch-result-object?id=c1"))
                .andRespond(withSuccess("{\"resultObject\":null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client("key", "agent", 1_000).search(CRITERIA))
                .isInstanceOfSatisfying(ScraperException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
