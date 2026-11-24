package com.alumnisearch.dto;

import com.alumnisearch.domain.AlumniProfile;

public record AlumniProfileResponse(
        String name,
        String currentRole,
        String university,
        String location,
        String linkedinHeadline,
        Integer passoutYear,
        String profileUrl) {

    public static AlumniProfileResponse from(AlumniProfile profile) {
        return new AlumniProfileResponse(
                profile.getName(),
                profile.getCurrentRole(),
                profile.getUniversity(),
                profile.getLocation(),
                profile.getLinkedinHeadline(),
                profile.getPassoutYear(),
                profile.getProfileUrl());
    }
}
