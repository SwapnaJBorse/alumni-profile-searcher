package com.alumnisearch.scraper;

import java.util.List;

/**
 * Port for any LinkedIn profile source. The service only knows this interface, so PhantomBuster
 * can be swapped (or mocked) without touching business logic.
 */
public interface ProfileScraperClient {

    List<ScrapedProfile> search(ProfileSearchCriteria criteria);
}
