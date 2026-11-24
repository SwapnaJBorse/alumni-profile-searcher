package com.alumnisearch.repository;

import com.alumnisearch.domain.AlumniProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AlumniRepository extends JpaRepository<AlumniProfile, Long> {

    List<AlumniProfile> findAllByProfileUrlIn(Collection<String> profileUrls);
}
