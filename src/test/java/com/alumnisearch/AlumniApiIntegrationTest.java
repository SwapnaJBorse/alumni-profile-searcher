package com.alumnisearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end flow with the mock scraper and an in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestPropertySource(properties = "phantombuster.mock-enabled=true")
class AlumniApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchPersistsProfilesAndRepeatedSearchDoesNotDuplicateThem() throws Exception {
        String body = "{\"university\":\"Sandip University\",\"designation\":\"Software Engineer\",\"passoutYear\":2025}";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/alumni/search").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].university").value("Sandip University"))
                    .andExpect(jsonPath("$.data[0].passoutYear").value(2025));
        }

        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void swaggerDocsAreServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/alumni/search']").exists());
    }
}
