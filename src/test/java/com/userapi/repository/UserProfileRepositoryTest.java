package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import com.userapi.models.entity.UserStatus;
import com.userapi.repository.userprofile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/cleanup-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/schema.sql", "/test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/cleanup-test-data.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class UserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void findByUserId_WhenUserExists_ShouldReturnUser() {
        // Given
        String orgUuid = "org-1";
        String userUuid = "test-user-1";

        // When
        UserProfile result = userProfileRepository.findByUserId(orgUuid, userUuid);

        // Then
        assertNotNull(result);
        assertEquals("john.doe", result.getUsername());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("1234567890", result.getPhone());
        assertEquals(1, result.getPhoneCountryCode());
        assertEquals("VERIFIED", result.getPhoneVerificationStatus());
        assertEquals("VERIFIED", result.getEmailVerificationStatus());
        assertEquals(UserStatus.ACTIVE.getName(), result.getStatus());
        assertArrayEquals(new String[]{"job-profile-1"}, result.getJobProfileUuids());
    }

    @Test
    void findByUserId_WhenUserDoesNotExist_ShouldReturnNull() {
        // Given
        String orgUuid = "org-1";
        String userUuid = "non-existent-user";

        // When
        UserProfile result = userProfileRepository.findByUserId(orgUuid, userUuid);

        // Then
        assertNull(result);
    }

    @Test
    void findUserByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        String orgUuid = "org-1";
        String username = "john.doe";

        // When
        UserProfile result = userProfileRepository.findUserByUsername(orgUuid, username);

        // Then
        assertNotNull(result);
        assertEquals("test-user-1", result.getUserUuid());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
    }

    @Test
    void findUserByUsername_WhenUserDoesNotExist_ShouldReturnNull() {
        // Given
        String orgUuid = "org-1";
        String username = "non-existent-username";

        // When
        UserProfile result = userProfileRepository.findUserByUsername(orgUuid, username);

        // Then
        assertNull(result);
    }

    @Test
    void findUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Given
        String orgUuid = "org-1";
        String email = "john.doe@example.com";

        // When
        UserProfile result = userProfileRepository.findUserByEmail(orgUuid, email);

        // Then
        assertNotNull(result);
        assertEquals("test-user-1", result.getUserUuid());
        assertEquals("john.doe", result.getUsername());
    }

    @Test
    void findUserByEmail_WhenUserDoesNotExist_ShouldReturnNull() {
        // Given
        String orgUuid = "org-1";
        String email = "non-existent@example.com";

        // When
        UserProfile result = userProfileRepository.findUserByEmail(orgUuid, email);

        // Then
        assertNull(result);
    }

    @Test
    void save_WhenNewUser_ShouldCreateUser() {
        // Given
        UserProfile newUser = new UserProfile();
        newUser.setUserUuid("test-user-3");
        newUser.setOrganizationUuid("org-1");
        newUser.setUsername("new.user");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEmail("new.user@example.com");
        newUser.setPhone("5555555555");
        newUser.setPhoneCountryCode(1);
        newUser.setPhoneVerificationStatus("VERIFIED");
        newUser.setEmailVerificationStatus("VERIFIED");
        newUser.setStatus(UserStatus.ACTIVE.getName());
        newUser.setJobProfileUuids(new String[]{"job-profile-1"});

        // When
        UserProfile savedUser = userProfileRepository.save(newUser);

        // Then
        assertNotNull(savedUser);
        assertEquals("test-user-3", savedUser.getUserUuid());
        assertEquals("new.user", savedUser.getUsername());
        assertEquals("New", savedUser.getFirstName());
        assertEquals("User", savedUser.getLastName());
        assertEquals("new.user@example.com", savedUser.getEmail());
        assertEquals("5555555555", savedUser.getPhone());
        assertEquals(1, savedUser.getPhoneCountryCode());
        assertEquals("VERIFIED", savedUser.getPhoneVerificationStatus());
        assertEquals("VERIFIED", savedUser.getEmailVerificationStatus());
        assertEquals(UserStatus.ACTIVE.getName(), savedUser.getStatus());
        assertArrayEquals(new String[]{"job-profile-1"}, savedUser.getJobProfileUuids());
    }

    @Test
    void save_WhenExistingUser_ShouldUpdateUser() {
        // Given
        String orgUuid = "org-1";
        String userUuid = "test-user-1";
        UserProfile existingUser = userProfileRepository.findByUserId(orgUuid, userUuid);
        existingUser.setFirstName("Updated");
        existingUser.setLastName("Name");

        // When
        UserProfile updatedUser = userProfileRepository.save(existingUser);

        // Then
        assertNotNull(updatedUser);
        assertEquals(userUuid, updatedUser.getUserUuid());
        assertEquals("Updated", updatedUser.getFirstName());
        assertEquals("Name", updatedUser.getLastName());
    }

    @Test
    void delete_WhenUserExists_ShouldDeleteUser() {
        // Given
        String orgUuid = "org-1";
        String userUuid = "test-user-1";
        UserProfile user = userProfileRepository.findByUserId(orgUuid, userUuid);

        // When
        userProfileRepository.delete(user);

        // Then
        UserProfile deletedUser = userProfileRepository.findByUserId(orgUuid, userUuid);
        assertNull(deletedUser);
    }
}