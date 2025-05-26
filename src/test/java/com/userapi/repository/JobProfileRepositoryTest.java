package com.userapi.repository;

import com.userapi.models.entity.JobProfile;
import com.userapi.repository.jobprofile.JobProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/cleanup-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/schema.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/cleanup-test-data.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class JobProfileRepositoryTest {

    @Autowired
    private JobProfileRepository jobProfileRepository;

    @Test
    @DisplayName("Save JobProfile and retrieve by ID")
    public void testSaveAndFindById() {
        // Given
        JobProfile jobProfile = JobProfile.builder()
                .jobProfileUuid("job1")
                .title("Software Engineer")
                .organizationUnit("Engineering")
                .organizationUuid("org1")
                .startDate(LocalDateTime.now())
                .build();

        // When
        JobProfile saved = jobProfileRepository.save(jobProfile);
        Optional<JobProfile> retrieved = jobProfileRepository.findById("job1");

        // Then
        assertThat(saved).isNotNull();
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTitle()).isEqualTo("Software Engineer");
        assertThat(retrieved.get().getOrganizationUnit()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Delete JobProfile")
    public void testDelete() {
        // Given
        JobProfile jobProfile = JobProfile.builder()
                .jobProfileUuid("job2")
                .title("QA Engineer")
                .organizationUuid("org1")
                .startDate(LocalDateTime.now())
                .build();

        // When
        jobProfileRepository.save(jobProfile);
        assertThat(jobProfileRepository.findById("job2")).isPresent();

        // Then
        jobProfileRepository.deleteById("job2");
        assertThat(jobProfileRepository.findById("job2")).isNotPresent();
    }

    // Additional tests can be added to test JpaSpecificationExecutor features, if used
}
