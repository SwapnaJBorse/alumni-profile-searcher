package com.alumnisearch.repository;

import com.alumnisearch.domain.AlumniProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AlumniRepositoryTest {

    @Autowired
    private AlumniRepository repository;

    private AlumniProfile profile(String name, String url) {
        AlumniProfile p = new AlumniProfile();
        p.setName(name);
        p.setProfileUrl(url);
        return p;
    }

    @Test
    void findsProfilesByUrls() {
        repository.save(profile("A", "https://www.linkedin.com/in/a"));
        repository.save(profile("B", "https://www.linkedin.com/in/b"));

        List<AlumniProfile> found = repository.findAllByProfileUrlIn(List.of("https://www.linkedin.com/in/a"));

        assertThat(found).extracting(AlumniProfile::getName).containsExactly("A");
        assertThat(found.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void profileUrlMustBeUnique() {
        repository.saveAndFlush(profile("A", "https://www.linkedin.com/in/a"));

        assertThatThrownBy(() -> repository.saveAndFlush(profile("A2", "https://www.linkedin.com/in/a")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSeveralProfilesWithoutUrl() {
        repository.save(profile("A", null));
        repository.save(profile("B", null));

        assertThat(repository.findAll()).hasSize(2);
    }
}
