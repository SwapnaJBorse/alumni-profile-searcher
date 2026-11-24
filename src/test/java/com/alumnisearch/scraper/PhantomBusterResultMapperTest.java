package com.alumnisearch.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhantomBusterResultMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PhantomBusterResultMapper mapper = new PhantomBusterResultMapper();

    private JsonNode json(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }

    @Test
    void mapsStandardFieldsAndDerivesRoleFromHeadline() throws Exception {
        List<ScrapedProfile> result = mapper.map(json("""
                [{"fullName":"John Doe","headline":"Software Engineer at XYZ Corp",
                  "location":"New York, NY","profileUrl":"https://www.linkedin.com/in/john-doe/?miniProfileUrn=abc"}]
                """));

        assertThat(result).singleElement().satisfies(p -> {
            assertThat(p.name()).isEqualTo("John Doe");
            assertThat(p.currentRole()).isEqualTo("Software Engineer");
            assertThat(p.location()).isEqualTo("New York, NY");
            assertThat(p.profileUrl()).isEqualTo("https://www.linkedin.com/in/john-doe");
        });
    }

    @Test
    void prefersExplicitJobTitleOverHeadline() throws Exception {
        List<ScrapedProfile> result = mapper.map(json("""
                [{"fullName":"A B","headline":"Dreamer | Builder","jobTitle":"Backend Developer"}]
                """));

        assertThat(result.get(0).currentRole()).isEqualTo("Backend Developer");
    }

    @Test
    void buildsNameFromFirstAndLastName() throws Exception {
        List<ScrapedProfile> result = mapper.map(json("""
                [{"firstName":"Jane","lastName":"Smith"}]
                """));

        assertThat(result.get(0).name()).isEqualTo("Jane Smith");
    }

    @Test
    void extractsPassoutYearFromEndYearFieldOrHeadline() throws Exception {
        List<ScrapedProfile> result = mapper.map(json("""
                [{"fullName":"A","schoolEndYear":"2020"},
                 {"fullName":"B","headline":"Engineer | Class of 2019"},
                 {"fullName":"C","headline":"Engineer"}]
                """));

        assertThat(result).extracting(ScrapedProfile::passoutYear).containsExactly(2020, 2019, null);
    }

    @Test
    void skipsRowsWithoutAName() throws Exception {
        assertThat(mapper.map(json("[{\"headline\":\"No name\"}, {}]"))).isEmpty();
    }

    @Test
    void returnsEmptyListForNonArrayInput() throws Exception {
        assertThat(mapper.map(json("{\"not\":\"an array\"}"))).isEmpty();
        assertThat(mapper.map(null)).isEmpty();
    }
}
