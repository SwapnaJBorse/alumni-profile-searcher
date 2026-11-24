package com.alumnisearch.controller;

import com.alumnisearch.dto.AlumniProfileResponse;
import com.alumnisearch.dto.AlumniSearchRequest;
import com.alumnisearch.exception.ScraperException;
import com.alumnisearch.service.AlumniService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlumniController.class)
class AlumniControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlumniService alumniService;

    private static final AlumniProfileResponse JOHN = new AlumniProfileResponse(
            "John Doe", "Software Engineer", "University of XYZ", "New York, NY",
            "Passionate Software Engineer at XYZ Corp", 2020, "https://www.linkedin.com/in/john");

    @Test
    void searchReturnsSuccessEnvelope() throws Exception {
        when(alumniService.searchAndSave(any(AlumniSearchRequest.class))).thenReturn(List.of(JOHN));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"university":"University of XYZ","designation":"Software Engineer","passoutYear":"2020"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].name").value("John Doe"))
                .andExpect(jsonPath("$.data[0].currentRole").value("Software Engineer"))
                .andExpect(jsonPath("$.data[0].passoutYear").value(2020));
    }

    @Test
    void searchAcceptsMissingPassoutYear() throws Exception {
        when(alumniService.searchAndSave(any(AlumniSearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\"XYZ\",\"designation\":\"Engineer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void searchRejectsBlankFieldsWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\" \",\"designation\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors.length()").value(2));
        verifyNoInteractions(alumniService);
    }

    @Test
    void searchRejectsOutOfRangeYear() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\"XYZ\",\"designation\":\"Engineer\",\"passoutYear\":1800}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("passoutYear: passoutYear must be 1950 or later"));
    }

    @Test
    void searchRejectsNonNumericYearAndMalformedJson() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\"XYZ\",\"designation\":\"Engineer\",\"passoutYear\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scraperFailureIsReportedWithItsStatus() throws Exception {
        when(alumniService.searchAndSave(any(AlumniSearchRequest.class)))
                .thenThrow(new ScraperException("PhantomBuster request failed with HTTP 500", HttpStatus.BAD_GATEWAY));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\"XYZ\",\"designation\":\"Engineer\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("PhantomBuster request failed with HTTP 500"));
    }

    @Test
    void unexpectedErrorsDoNotLeakDetails() throws Exception {
        when(alumniService.searchAndSave(any(AlumniSearchRequest.class)))
                .thenThrow(new IllegalStateException("secret internals"));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\":\"XYZ\",\"designation\":\"Engineer\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void wrongMethodReturnsJson405() throws Exception {
        mockMvc.perform(get("/api/alumni/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void allReturnsSavedProfiles() throws Exception {
        when(alumniService.findAll()).thenReturn(List.of(JOHN));

        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
