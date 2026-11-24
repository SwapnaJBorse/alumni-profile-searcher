package com.alumnisearch.service;

import com.alumnisearch.dto.AlumniProfileResponse;
import com.alumnisearch.dto.AlumniSearchRequest;

import java.util.List;

public interface AlumniService {

    /** Scrapes matching alumni, upserts them in the database and returns the stored profiles. */
    List<AlumniProfileResponse> searchAndSave(AlumniSearchRequest request);

    List<AlumniProfileResponse> findAll();
}
