package com.alumnisearch.scraper;

import com.alumnisearch.config.PhantomBusterProperties;
import com.alumnisearch.exception.ScraperException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * PhantomBuster API v2 adapter: launch the agent, poll its container until finished, read the result object.
 */
@Component
@ConditionalOnProperty(name = "phantombuster.mock-enabled", havingValue = "false", matchIfMissing = true)
public class PhantomBusterClient implements ProfileScraperClient {

    private static final Logger log = LoggerFactory.getLogger(PhantomBusterClient.class);
    private static final String API_KEY_HEADER = "X-Phantombuster-Key";

    private final RestClient restClient;
    private final PhantomBusterProperties properties;
    private final PhantomBusterResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    public PhantomBusterClient(RestClient.Builder builder,
                               PhantomBusterProperties properties,
                               PhantomBusterResultMapper resultMapper,
                               ObjectMapper objectMapper) {
        this.properties = properties;
        this.resultMapper = resultMapper;
        this.objectMapper = objectMapper;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(API_KEY_HEADER, properties.apiKey() == null ? "" : properties.apiKey())
                .build();
    }

    @Override
    public List<ScrapedProfile> search(ProfileSearchCriteria criteria) {
        assertConfigured();
        String containerId = launch(criteria);
        log.info("PhantomBuster container {} launched for '{}' / '{}'",
                containerId, criteria.university(), criteria.designation());
        awaitCompletion(containerId);
        return resultMapper.map(fetchResultRows(containerId));
    }

    private void assertConfigured() {
        if (isBlank(properties.apiKey()) || isBlank(properties.agentId())) {
            throw new ScraperException(
                    "PhantomBuster is not configured: set PHANTOMBUSTER_API_KEY and PHANTOMBUSTER_AGENT_ID",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String launch(ProfileSearchCriteria criteria) {
        Map<String, Object> body = Map.of("id", properties.agentId(), "argument", buildArgument(criteria));
        JsonNode response = call(() -> restClient.post()
                .uri("/agents/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
        String containerId = response == null ? null : response.path("containerId").asText(null);
        if (isBlank(containerId)) {
            throw new ScraperException("PhantomBuster did not return a container id", HttpStatus.BAD_GATEWAY);
        }
        return containerId;
    }

    /**
     * Argument keys follow the "LinkedIn Search Export" phantom. If your agent uses other names, copy the JSON
     * shown under the agent's "Launch via API" section and adjust the keys here - this is the only place to change.
     */
    private Map<String, Object> buildArgument(ProfileSearchCriteria criteria) {
        Map<String, Object> argument = new LinkedHashMap<>();
        argument.put("sessionCookie", properties.sessionCookie());
        argument.put("searches", LinkedInSearchUrlBuilder.build(criteria));
        argument.put("category", "People");
        argument.put("numberOfResultsPerSearch", properties.maxProfiles());
        return argument;
    }

    private void awaitCompletion(String containerId) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(properties.timeoutMs());
        while (true) {
            JsonNode output = call(() -> restClient.get()
                    .uri("/containers/fetch-output?id={id}", containerId)
                    .retrieve()
                    .body(JsonNode.class));
            if (output != null && "finished".equalsIgnoreCase(output.path("status").asText())) {
                return;
            }
            if (System.nanoTime() >= deadline) {
                throw new ScraperException(
                        "PhantomBuster did not finish within " + properties.timeoutMs() + " ms",
                        HttpStatus.GATEWAY_TIMEOUT);
            }
            pause();
        }
    }

    private JsonNode fetchResultRows(String containerId) {
        JsonNode response = call(() -> restClient.get()
                .uri("/containers/fetch-result-object?id={id}", containerId)
                .retrieve()
                .body(JsonNode.class));
        JsonNode result = response == null ? null : response.path("resultObject");
        if (result == null || result.isMissingNode() || result.isNull()) {
            throw new ScraperException(
                    "PhantomBuster finished without results; check the agent logs and LinkedIn session cookie",
                    HttpStatus.BAD_GATEWAY);
        }
        if (result.isTextual()) {
            try {
                result = objectMapper.readTree(result.asText());
            } catch (JsonProcessingException ex) {
                throw new ScraperException("PhantomBuster returned an unreadable result",
                        HttpStatus.BAD_GATEWAY, ex);
            }
        }
        if (!result.isArray()) {
            throw new ScraperException("PhantomBuster returned an unexpected result format", HttpStatus.BAD_GATEWAY);
        }
        return result;
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException ex) {
            throw new ScraperException(
                    "PhantomBuster request failed with HTTP " + ex.getStatusCode().value(),
                    HttpStatus.BAD_GATEWAY, ex);
        } catch (RestClientException ex) {
            throw new ScraperException("Unable to reach PhantomBuster", HttpStatus.BAD_GATEWAY, ex);
        }
    }

    private void pause() {
        try {
            Thread.sleep(properties.pollIntervalMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ScraperException("Interrupted while waiting for PhantomBuster",
                    HttpStatus.SERVICE_UNAVAILABLE, ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
