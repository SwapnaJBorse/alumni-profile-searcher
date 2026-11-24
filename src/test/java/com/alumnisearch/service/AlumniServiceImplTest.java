package com.alumnisearch.service;

import com.alumnisearch.domain.AlumniProfile;
import com.alumnisearch.dto.AlumniProfileResponse;
import com.alumnisearch.dto.AlumniSearchRequest;
import com.alumnisearch.exception.ScraperException;
import com.alumnisearch.repository.AlumniRepository;
import com.alumnisearch.scraper.ProfileScraperClient;
import com.alumnisearch.scraper.ProfileSearchCriteria;
import com.alumnisearch.scraper.ScrapedProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumniServiceImplTest {

    @Mock
    private ProfileScraperClient scraperClient;
    @Mock
    private AlumniRepository repository;

    private AlumniServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlumniServiceImpl(scraperClient, repository);
    }

    @Test
    void savesNewProfilesAndReturnsThem() {
        when(scraperClient.search(any())).thenReturn(List.of(
                new ScrapedProfile("Jane Smith", "Data Scientist | AI", "Data Scientist",
                        "San Francisco, CA", "https://www.linkedin.com/in/jane", 2019)));
        when(repository.findAllByProfileUrlIn(anyCollection())).thenReturn(List.of());
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AlumniProfileResponse> result = service.searchAndSave(
                new AlumniSearchRequest("  University of XYZ ", "Data Scientist", null));

        assertThat(result).singleElement().satisfies(profile -> {
            assertThat(profile.name()).isEqualTo("Jane Smith");
            assertThat(profile.university()).isEqualTo("University of XYZ");
            assertThat(profile.passoutYear()).isEqualTo(2019);
        });
    }

    @Test
    void updatesExistingProfileInsteadOfCreatingDuplicate() {
        AlumniProfile existing = new AlumniProfile();
        existing.setName("Old Name");
        existing.setProfileUrl("https://www.linkedin.com/in/jane");
        when(scraperClient.search(any())).thenReturn(List.of(
                new ScrapedProfile("Jane Smith", "New headline", "Engineer", "Pune",
                        "https://www.linkedin.com/in/jane", null)));
        when(repository.findAllByProfileUrlIn(anyCollection())).thenReturn(List.of(existing));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.searchAndSave(new AlumniSearchRequest("XYZ", "Engineer", 2020));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlumniProfile>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().isSameAs(existing);
        assertThat(existing.getName()).isEqualTo("Jane Smith");
        assertThat(existing.getLinkedinHeadline()).isEqualTo("New headline");
        assertThat(existing.getPassoutYear()).isEqualTo(2020);
    }

    @Test
    void collapsesDuplicatesReturnedByScraper() {
        ScrapedProfile profile = new ScrapedProfile("Jane", "h", "r", "l", "https://www.linkedin.com/in/jane", null);
        when(scraperClient.search(any())).thenReturn(List.of(profile, profile));
        when(repository.findAllByProfileUrlIn(anyCollection())).thenReturn(List.of());
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.searchAndSave(new AlumniSearchRequest("XYZ", "Engineer", null))).hasSize(1);
    }

    @Test
    void returnsEmptyListWithoutTouchingDatabaseWhenNothingFound() {
        when(scraperClient.search(any())).thenReturn(List.of());

        assertThat(service.searchAndSave(new AlumniSearchRequest("XYZ", "Engineer", null))).isEmpty();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void passesTrimmedCriteriaToScraper() {
        when(scraperClient.search(any())).thenReturn(List.of());

        service.searchAndSave(new AlumniSearchRequest(" XYZ ", " Engineer ", 2021));

        ArgumentCaptor<ProfileSearchCriteria> captor = ArgumentCaptor.forClass(ProfileSearchCriteria.class);
        verify(scraperClient).search(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ProfileSearchCriteria("XYZ", "Engineer", 2021));
    }

    @Test
    void propagatesScraperFailures() {
        when(scraperClient.search(any()))
                .thenThrow(new ScraperException("boom", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.searchAndSave(new AlumniSearchRequest("XYZ", "Engineer", null)))
                .isInstanceOf(ScraperException.class)
                .hasMessage("boom");
        verify(repository, never()).saveAll(any());
    }
}
