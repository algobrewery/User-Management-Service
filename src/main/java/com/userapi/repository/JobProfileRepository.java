package com.userapi.repository;

import com.userapi.models.entity.JobProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobProfileRepository extends JpaRepository<JobProfile, String> {
}