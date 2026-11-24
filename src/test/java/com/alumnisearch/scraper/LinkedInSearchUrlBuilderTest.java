package com.alumnisearch.scraper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedInSearchUrlBuilderTest {

    @Test
    void encodesKeywordsAndIncludesYearWhenProvided() {
        String url = LinkedInSearchUrlBuilder.build(
                new ProfileSearchCriteria("Sandip University", "Software Engineer", 2025));

        assertThat(url).isEqualTo("https://www.linkedin.com/search/results/people/"
                + "?keywords=Software%20Engineer%20Sandip%20University%202025&origin=GLOBAL_SEARCH_HEADER");
    }

    @Test
    void omitsYearWhenAbsentAndEscapesSpecialCharacters() {
        String url = LinkedInSearchUrlBuilder.build(new ProfileSearchCriteria("A&M", "Dev", null));

        assertThat(url).contains("keywords=Dev%20A%26M&");
    }
}
