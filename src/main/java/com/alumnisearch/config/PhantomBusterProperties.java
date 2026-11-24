package com.alumnisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalised configuration for the PhantomBuster integration.
 * Secrets (API key, LinkedIn session cookie) come from environment variables, never from source control.
 */
@ConfigurationProperties(prefix = "phantombuster")
public record PhantomBusterProperties(
        @DefaultValue("https://api.phantombuster.com/api/v2") String baseUrl,
        String apiKey,
        String agentId,
        String sessionCookie,
        @DefaultValue("false") boolean mockEnabled,
        @DefaultValue("3000") long pollIntervalMs,
        @DefaultValue("180000") long timeoutMs,
        @DefaultValue("25") int maxProfiles) {
}
