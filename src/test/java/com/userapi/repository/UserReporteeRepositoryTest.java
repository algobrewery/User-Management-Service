package com.userapi.repository;

import com.userapi.models.entity.UserReportee;
import com.userapi.repository.userreportee.UserReporteeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/cleanup-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/schema.sql", "/test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/cleanup-test-data.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class UserReporteeRepositoryTest {

    @Autowired
    private UserReporteeRepository userReporteeRepository;

    @Test
    void findByManagerUserUuid_WhenManagerExists_ShouldReturnReportees() {
        // Given
        String orgUuid = "org-1";
        String managerUuid = "test-user-2"; // Jane Smith is the manager

        // When
        List<UserReportee> reportees = userReporteeRepository.findByManagerUserUuid(orgUuid, managerUuid);

        // Then
        assertNotNull(reportees);
        assertEquals(2, reportees.size()); // Now we have 2 reportees for test-user-2

        // Verify that both test-user-1 and test-user-3 are reportees
        boolean foundUser1 = false;
        boolean foundUser3 = false;

        for (UserReportee reportee : reportees) {
            if ("test-user-1".equals(reportee.getUserUuid())) {
                foundUser1 = true;
            } else if ("test-user-3".equals(reportee.getUserUuid())) {
                foundUser3 = true;
            }
            assertEquals(managerUuid, reportee.getManagerUserUuid());
            assertEquals(orgUuid, reportee.getOrganizationUuid());
        }

        assertTrue(foundUser1, "test-user-1 should be a reportee");
        assertTrue(foundUser3, "test-user-3 should be a reportee");
    }

    @Test
    void findByManagerUserUuid_WhenManagerDoesNotExist_ShouldReturnEmptyList() {
        // Given
        String orgUuid = "org-1";
        String managerUuid = "non-existent-manager";

        // When
        List<UserReportee> reportees = userReporteeRepository.findByManagerUserUuid(orgUuid, managerUuid);

        // Then
        assertNotNull(reportees);
        assertTrue(reportees.isEmpty());
    }

    @Test
    void findByJobProfileUuid_WhenJobProfileExists_ShouldReturnReportees() {
        // Given
        String orgUuid = "org-1";
        String jobProfileUuid = "job-profile-1";

        // When
        List<UserReportee> reportees = userReporteeRepository.findByJobProfileUuid(orgUuid, jobProfileUuid);

        // Then
        assertNotNull(reportees);
        assertEquals(1, reportees.size());

        UserReportee reportee = reportees.get(0);
        assertEquals("test-user-1", reportee.getUserUuid());
        assertEquals(jobProfileUuid, reportee.getJobProfileUuid());
        assertEquals(orgUuid, reportee.getOrganizationUuid());
    }

    @Test
    void findByJobProfileUuid_WhenJobProfileDoesNotExist_ShouldReturnEmptyList() {
        // Given
        String orgUuid = "org-1";
        String jobProfileUuid = "non-existent-job-profile";

        // When
        List<UserReportee> reportees = userReporteeRepository.findByJobProfileUuid(orgUuid, jobProfileUuid);

        // Then
        assertNotNull(reportees);
        assertTrue(reportees.isEmpty());
    }

    @Test
    @Transactional
    void deleteByJobProfileUuid_WhenJobProfileExists_ShouldDeleteReportees() {
        // Given
        String orgUuid = "org-1";
        String jobProfileUuid = "job-profile-1";

        // Verify reportees exist before deletion
        List<UserReportee> reporteesBefore = userReporteeRepository.findByJobProfileUuid(orgUuid, jobProfileUuid);
        assertFalse(reporteesBefore.isEmpty());

        // When
        userReporteeRepository.deleteByJobProfileUuid(jobProfileUuid);

        // Then
        List<UserReportee> reporteesAfter = userReporteeRepository.findByJobProfileUuid(orgUuid, jobProfileUuid);
        assertTrue(reporteesAfter.isEmpty());
    }

    @Test
    @Transactional
    void save_WhenNewReportee_ShouldCreateReportee() {
        // Given
        UserReportee newReportee = UserReportee.builder()
                .relationUuid(UUID.randomUUID().toString())
                .userUuid("test-user-3")
                .managerUserUuid("test-user-2")
                .organizationUuid("org-1")
                .jobProfileUuid("job-profile-1")
                .build();

        // When
        UserReportee savedReportee = userReporteeRepository.save(newReportee);

        // Then
        assertNotNull(savedReportee);
        assertEquals("test-user-3", savedReportee.getUserUuid());
        assertEquals("test-user-2", savedReportee.getManagerUserUuid());
        assertEquals("org-1", savedReportee.getOrganizationUuid());
        assertEquals("job-profile-1", savedReportee.getJobProfileUuid());
    }

    @Test
    @Transactional
    void delete_WhenReporteeExists_ShouldDeleteReportee() {
        // Given
        String orgUuid = "org-1";
        String jobProfileUuid = "job-profile-1";
        List<UserReportee> reportees = userReporteeRepository.findByJobProfileUuid(orgUuid, jobProfileUuid);
        assertFalse(reportees.isEmpty());
        UserReportee reportee = reportees.get(0);

        // When
        userReporteeRepository.delete(reportee);

        // Then
        List<UserReportee> reporteesAfter = userReporteeRepository.findByJobProfileUuid(orgUuid, jobProfileUuid);
        assertTrue(reporteesAfter.isEmpty());
    }
}