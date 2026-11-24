package com.alumnisearch.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anti-corruption layer: converts PhantomBuster's loosely-typed result rows into {@link ScrapedProfile}.
 * Field names differ slightly between phantoms/versions, so several aliases are accepted for each value.
 */
@Component
public class PhantomBusterResultMapper {

    private static final Pattern YEAR_IN_HEADLINE =
            Pattern.compile("(?i)\\b(?:class|batch)\\s+of\\s+((?:19|20)\\d{2})\\b");
    private static final Pattern HEADLINE_SPLIT = Pattern.compile("\\s+(?:at|@)\\s+|\\s*\\|\\s*");

    public List<ScrapedProfile> map(JsonNode rows) {
        List<ScrapedProfile> profiles = new ArrayList<>();
        if (rows == null || !rows.isArray()) {
            return profiles;
        }
        for (JsonNode row : rows) {
            toProfile(row).ifPresent(profiles::add);
        }
        return profiles;
    }

    private Optional<ScrapedProfile> toProfile(JsonNode row) {
        String name = firstNonBlank(
                text(row, "fullName"),
                text(row, "name"),
                joinNames(text(row, "firstName"), text(row, "lastName")));
        if (name == null) {
            return Optional.empty();
        }
        String headline = firstNonBlank(text(row, "headline"), text(row, "job"), text(row, "title"));
        String role = firstNonBlank(text(row, "jobTitle"), deriveRole(headline));
        String location = text(row, "location");
        String url = normalizeUrl(firstNonBlank(
                text(row, "profileUrl"), text(row, "linkedinProfileUrl"),
                text(row, "defaultProfileUrl"), text(row, "url")));
        Integer year = firstNonNull(
                parseYear(text(row, "passoutYear")),
                parseYear(text(row, "schoolEndYear")),
                parseYear(text(row, "endYear")),
                yearFromHeadline(headline));
        return Optional.of(new ScrapedProfile(name, headline, role, location, url, year));
    }

    private static String deriveRole(String headline) {
        if (headline == null) {
            return null;
        }
        String first = HEADLINE_SPLIT.split(headline, 2)[0].trim();
        return first.isEmpty() ? null : first;
    }

    private static Integer yearFromHeadline(String headline) {
        if (headline == null) {
            return null;
        }
        Matcher matcher = YEAR_IN_HEADLINE.matcher(headline);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private static Integer parseYear(String value) {
        if (value == null) {
            return null;
        }
        try {
            int year = Integer.parseInt(value.trim());
            return year >= 1950 && year <= 2100 ? year : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Drops tracking query strings and trailing slashes so the same person always yields the same key. */
    private static String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String cleaned = url.split("\\?", 2)[0];
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static String joinNames(String first, String last) {
        return firstNonBlank(((first == null ? "" : first) + " " + (last == null ? "" : last)).trim());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
