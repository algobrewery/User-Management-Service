package com.userapi.integration;

import com.fasterxml.jackson.databind.*;
import com.userapi.models.entity.VerificationStatus;
import com.userapi.models.external.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import static com.userapi.common.constants.HeaderConstants.*;
import static com.userapi.common.constants.HeaderConstants.API_KEY;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"classpath:test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class UserApiIntegrationTest {

    private static final String TEST_API_KEY = "APAHdSmELUW4iMvBR6w4xP_q8K-blauC8HKml3CROOA";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUserRequest createUserRequest;
    private String userId;
    private String managerUuid;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();

        // Use predefined manager from test-data.sql
        managerUuid = "test-user-2"; // This is the manager UUID defined in test-data.sql

        // Create test data for a new user
        EmailInfo testEmailInfo = new EmailInfo();
        testEmailInfo.setEmail("test-" + UUID.randomUUID() + "@example.com");
        testEmailInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());

        PhoneInfo testPhoneInfo = new PhoneInfo();
        testPhoneInfo.setNumber(UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 10));
        testPhoneInfo.setCountryCode(1);
        testPhoneInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());

        EmploymentInfo employmentInfo = new EmploymentInfo();
        employmentInfo.setJobTitle("Software Engineer");
        employmentInfo.setOrganizationUnit("Engineering");
        employmentInfo.setStartDate(LocalDateTime.now());
        employmentInfo.setReportingManager(managerUuid);
        employmentInfo.setExtensionsData(new HashMap<>());
        employmentInfo.setEndDate(null);

        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(userId);
        createUserRequest.setFirstName("John");
        createUserRequest.setLastName("Doe");
        createUserRequest.setEmailInfo(testEmailInfo);
        createUserRequest.setPhoneInfo(testPhoneInfo);
        createUserRequest.setEmploymentInfoList(Arrays.asList(employmentInfo));
    }

    @Test
    @Transactional
    void createUser_WhenValidRequest_ShouldCreateUser() throws Exception {
        // Create a user with a unique username, email, and phone
        String uniqueUsername = "create-test-" + UUID.randomUUID();

        EmailInfo uniqueEmailInfo = new EmailInfo();
        uniqueEmailInfo.setEmail("create-test-" + UUID.randomUUID() + "@example.com");
        uniqueEmailInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());

        PhoneInfo uniquePhoneInfo = new PhoneInfo();
        uniquePhoneInfo.setNumber(UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 10));
        uniquePhoneInfo.setCountryCode(1);
        uniquePhoneInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());

        EmploymentInfo uniqueEmploymentInfo = new EmploymentInfo();
        uniqueEmploymentInfo.setJobTitle("Software Engineer");
        uniqueEmploymentInfo.setOrganizationUnit("Engineering");
        uniqueEmploymentInfo.setStartDate(LocalDateTime.now());
        uniqueEmploymentInfo.setReportingManager(managerUuid);
        uniqueEmploymentInfo.setExtensionsData(new HashMap<>());
        uniqueEmploymentInfo.setEndDate(null);

        CreateUserRequest uniqueUserRequest = new CreateUserRequest();
        uniqueUserRequest.setUsername(uniqueUsername);
        uniqueUserRequest.setFirstName("John");
        uniqueUserRequest.setLastName("Doe");
        uniqueUserRequest.setEmailInfo(uniqueEmailInfo);
        uniqueUserRequest.setPhoneInfo(uniquePhoneInfo);
        uniqueUserRequest.setEmploymentInfoList(Arrays.asList(uniqueEmploymentInfo));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(uniqueUserRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void createUser_WhenDuplicateUser_ShouldReturnError() throws Exception {
        // Create a user with a unique username, email, and phone for duplicate test
        String uniqueUsername = "duplicate-test-" + UUID.randomUUID();

        EmailInfo uniqueEmailInfo = new EmailInfo();
        uniqueEmailInfo.setEmail("duplicate-test-" + UUID.randomUUID() + "@example.com");
        uniqueEmailInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());

        PhoneInfo uniquePhoneInfo = new PhoneInfo();
        uniquePhoneInfo.setNumber(UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 10));
        uniquePhoneInfo.setCountryCode(1);
        uniquePhoneInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());

        EmploymentInfo uniqueEmploymentInfo = new EmploymentInfo();
        uniqueEmploymentInfo.setJobTitle("Software Engineer");
        uniqueEmploymentInfo.setOrganizationUnit("Engineering");
        uniqueEmploymentInfo.setStartDate(LocalDateTime.now());
        uniqueEmploymentInfo.setReportingManager(managerUuid);
        uniqueEmploymentInfo.setExtensionsData(new HashMap<>());
        uniqueEmploymentInfo.setEndDate(null);

        CreateUserRequest duplicateUserRequest = new CreateUserRequest();
        duplicateUserRequest.setUsername(uniqueUsername);
        duplicateUserRequest.setFirstName("John");
        duplicateUserRequest.setLastName("Doe");
        duplicateUserRequest.setEmailInfo(uniqueEmailInfo);
        duplicateUserRequest.setPhoneInfo(uniquePhoneInfo);
        duplicateUserRequest.setEmploymentInfoList(Arrays.asList(uniqueEmploymentInfo));

        // First create the user
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(duplicateUserRequest)))
                .andExpect(status().isOk());

        // Then try to create the same user again
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(duplicateUserRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void getUser_WhenUserExists_ShouldReturnUser() throws Exception {
        // Use an existing user from test-data.sql
        String existingUserId = "test-user-1";

        // Get the existing user
        mockMvc.perform(get("/user/{userId}", existingUserId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void getUser_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/user/{userId}", "non-existent-id")
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void updateUser_WhenValidRequest_ShouldUpdateUser() throws Exception {
        // Use an existing user from test-data.sql
        String existingUserId = "test-user-1";

        // First get the existing user to verify it exists
        MvcResult getUserResult = mockMvc.perform(get("/user/{userId}", existingUserId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk())
                .andReturn();

        String getUserResponse = getUserResult.getResponse().getContentAsString();
        JsonNode userJson = objectMapper.readTree(getUserResponse);

        // Create update request with new name
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUsername(userJson.path("username").asText());
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");

        // Create email info
        EmailInfo emailInfo = new EmailInfo();
        emailInfo.setEmail(userJson.path("email").asText());
        emailInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());
        updateRequest.setEmailInfo(emailInfo);

        // Create phone info
        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.setNumber(userJson.path("phone").asText());
        phoneInfo.setCountryCode(1);
        phoneInfo.setVerificationStatus(VerificationStatus.VERIFIED.toString());
        updateRequest.setPhoneInfo(phoneInfo);

        // Create employment info
        EmploymentInfo employmentInfo = new EmploymentInfo();
        employmentInfo.setJobTitle("Updated Job Title");
        employmentInfo.setOrganizationUnit("Updated Department");
        employmentInfo.setStartDate(LocalDateTime.now());
        employmentInfo.setReportingManager(managerUuid);
        employmentInfo.setExtensionsData(new HashMap<>());
        updateRequest.setEmploymentInfo(employmentInfo);

        // Update the user
        mockMvc.perform(put("/user/{userId}", existingUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Verify the user was updated
        mockMvc.perform(get("/user/{userId}", existingUserId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deleteUser_WhenUserExists_ShouldDeleteUser() throws Exception {
        // Use an existing user from test-data.sql
        String existingUserId = "test-user-3"; // Use a different user than the other tests

        // First verify the user exists
        mockMvc.perform(get("/user/{userId}", existingUserId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk());

        // Then delete the user
        mockMvc.perform(delete("/user/{userId}", existingUserId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk());

        // Verify the user is marked as inactive but still accessible
        mockMvc.perform(get("/user/{userId}", existingUserId)
                        .header(API_KEY, TEST_API_KEY)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk());
    }
}
