package com.userapi.repository.jobprofile;

import com.userapi.models.entity.JobProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JobProfileRepository extends JpaRepository<JobProfile, String>, JpaSpecificationExecutor<JobProfile> {
}