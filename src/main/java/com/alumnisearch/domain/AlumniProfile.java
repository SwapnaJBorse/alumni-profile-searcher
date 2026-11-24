package com.alumnisearch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alumni_profiles", indexes = @Index(name = "idx_alumni_university", columnList = "university"))
public class AlumniProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // "current_role" is a reserved word in PostgreSQL, so the column gets an explicit name.
    @Column(name = "job_title")
    private String currentRole;

    private String university;

    private String location;

    @Column(length = 1000)
    private String linkedinHeadline;

    private Integer passoutYear;

    /** Natural key used to avoid saving the same person twice. Nullable because a scrape may omit it. */
    @Column(unique = true, length = 500)
    private String profileUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getLinkedinHeadline() { return linkedinHeadline; }
    public void setLinkedinHeadline(String linkedinHeadline) { this.linkedinHeadline = linkedinHeadline; }
    public Integer getPassoutYear() { return passoutYear; }
    public void setPassoutYear(Integer passoutYear) { this.passoutYear = passoutYear; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
