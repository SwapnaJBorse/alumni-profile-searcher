package com.alumnisearch.service;

import com.alumnisearch.domain.AlumniProfile;
import com.alumnisearch.dto.AlumniProfileResponse;
import com.alumnisearch.dto.AlumniSearchRequest;
import com.alumnisearch.repository.AlumniRepository;
import com.alumnisearch.scraper.ProfileScraperClient;
import com.alumnisearch.scraper.ProfileSearchCriteria;
import com.alumnisearch.scraper.ScrapedProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The scrape can take minutes, so this class is deliberately NOT @Transactional: no DB connection is held
 * while waiting. Each repository call below runs in its own short transaction.
 */
@Service
public class AlumniServiceImpl implements AlumniService {

    private static final Logger log = LoggerFactory.getLogger(AlumniServiceImpl.class);

    private final ProfileScraperClient scraperClient;
    private final AlumniRepository repository;

    public AlumniServiceImpl(ProfileScraperClient scraperClient, AlumniRepository repository) {
        this.scraperClient = scraperClient;
        this.repository = repository;
    }

    @Override
    public List<AlumniProfileResponse> searchAndSave(AlumniSearchRequest request) {
        ProfileSearchCriteria criteria = new ProfileSearchCriteria(
                request.university().trim(), request.designation().trim(), request.passoutYear());

        List<ScrapedProfile> scraped = deduplicate(scraperClient.search(criteria));
        if (scraped.isEmpty()) {
            log.info("No alumni found for '{}' / '{}'", criteria.university(), criteria.designation());
            return List.of();
        }

        Map<String, AlumniProfile> existingByUrl = loadExisting(scraped);
        List<AlumniProfile> toSave = scraped.stream()
                .map(profile -> applyTo(existingByUrl.get(profile.profileUrl()), profile, criteria))
                .toList();

        return repository.saveAll(toSave).stream().map(AlumniProfileResponse::from).toList();
    }

    @Override
    public List<AlumniProfileResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(AlumniProfileResponse::from).toList();
    }

    private Map<String, AlumniProfile> loadExisting(List<ScrapedProfile> scraped) {
        Set<String> urls = scraped.stream()
                .map(ScrapedProfile::profileUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, AlumniProfile> existing = new HashMap<>();
        if (!urls.isEmpty()) {
            repository.findAllByProfileUrlIn(urls).forEach(p -> existing.put(p.getProfileUrl(), p));
        }
        return existing;
    }

    private static List<ScrapedProfile> deduplicate(List<ScrapedProfile> profiles) {
        Map<String, ScrapedProfile> unique = new LinkedHashMap<>();
        for (ScrapedProfile profile : profiles) {
            String key = profile.profileUrl() != null
                    ? profile.profileUrl()
                    : "name:" + profile.name().toLowerCase();
            unique.putIfAbsent(key, profile);
        }
        return List.copyOf(unique.values());
    }

    private static AlumniProfile applyTo(AlumniProfile existing, ScrapedProfile scraped,
                                         ProfileSearchCriteria criteria) {
        AlumniProfile entity = existing != null ? existing : new AlumniProfile();
        entity.setName(scraped.name());
        entity.setCurrentRole(scraped.currentRole());
        entity.setLinkedinHeadline(scraped.headline());
        entity.setLocation(scraped.location());
        entity.setProfileUrl(scraped.profileUrl());
        entity.setUniversity(criteria.university());
        // Search exports rarely include graduation year; when none is found, keep the year the caller asked for.
        entity.setPassoutYear(scraped.passoutYear() != null ? scraped.passoutYear() : criteria.passoutYear());
        return entity;
    }
}
