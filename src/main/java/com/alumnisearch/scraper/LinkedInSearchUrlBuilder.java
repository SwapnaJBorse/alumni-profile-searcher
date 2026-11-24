package com.alumnisearch.scraper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Builds the LinkedIn people-search URL that the PhantomBuster search phantom will crawl. */
final class LinkedInSearchUrlBuilder {

    private static final String BASE = "https://www.linkedin.com/search/results/people/";

    private LinkedInSearchUrlBuilder() {
    }

    static String build(ProfileSearchCriteria criteria) {
        String keywords = Stream.of(
                        criteria.designation(),
                        criteria.university(),
                        criteria.passoutYear() == null ? null : String.valueOf(criteria.passoutYear()))
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        String encoded = URLEncoder.encode(keywords, StandardCharsets.UTF_8).replace("+", "%20");
        return BASE + "?keywords=" + encoded + "&origin=GLOBAL_SEARCH_HEADER";
    }
}
