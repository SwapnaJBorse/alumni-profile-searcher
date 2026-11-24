package com.alumnisearch.scraper;

/** Provider-neutral view of one scraped LinkedIn profile. */
public record ScrapedProfile(
        String name,
        String headline,
        String currentRole,
        String location,
        String profileUrl,
        Integer passoutYear) {
}
