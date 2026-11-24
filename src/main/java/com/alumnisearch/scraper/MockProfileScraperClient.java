package com.alumnisearch.scraper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic stand-in used when PHANTOMBUSTER_MOCK=true. Lets the deployed app and the demo video work
 * without a paid PhantomBuster plan or a live LinkedIn session.
 */
@Component
@ConditionalOnProperty(name = "phantombuster.mock-enabled", havingValue = "true")
public class MockProfileScraperClient implements ProfileScraperClient {

    private static final String[][] PEOPLE = {
            {"Aarav Sharma", "Pune, Maharashtra, India"},
            {"Priya Nair", "Bengaluru, Karnataka, India"},
            {"Rohan Mehta", "Mumbai, Maharashtra, India"}
    };

    @Override
    public List<ScrapedProfile> search(ProfileSearchCriteria criteria) {
        String slug = slugify(criteria.university() + "-" + criteria.designation());
        return java.util.Arrays.stream(PEOPLE)
                .map(person -> new ScrapedProfile(
                        person[0],
                        criteria.designation() + " at Example Corp",
                        criteria.designation(),
                        person[1],
                        "https://www.linkedin.com/in/mock-" + slugify(person[0]) + "-" + slug,
                        criteria.passoutYear()))
                .toList();
    }

    private static String slugify(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
