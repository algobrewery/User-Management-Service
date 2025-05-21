package com.userapi.integration;

import com.fasterxml.jackson.databind.*;
import com.userapi.models.external.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static com.userapi.common.constants.HeaderConstants.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserApiIntegrationTest {

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

        // Create test data for manager
        EmailInfo managerEmailInfo = new EmailInfo();
        managerEmailInfo.setEmail("manager@example.com");
        managerEmailInfo.setVerificationStatus("VERIFIED");

        PhoneInfo managerPhoneInfo = new PhoneInfo();
        managerPhoneInfo.setNumber("1234567890");
        managerPhoneInfo.setCountryCode(1);
        managerPhoneInfo.setVerificationStatus("VERIFIED");

        EmploymentInfo managerEmploymentInfo = new EmploymentInfo();
        managerEmploymentInfo.setJobTitle("Manager");
        managerEmploymentInfo.setOrganizationUnit("Management");
        managerEmploymentInfo.setStartDate(LocalDateTime.now());
        managerEmploymentInfo.setReportingManager("");
        managerEmploymentInfo.setExtensionsData(new HashMap<>());
        managerEmploymentInfo.setEndDate(null);

        CreateUserRequest managerRequest = new CreateUserRequest();
        managerRequest.setUsername("manager-1");
        managerRequest.setFirstName("Manager");
        managerRequest.setLastName("One");
        managerRequest.setEmailInfo(managerEmailInfo);
        managerRequest.setPhoneInfo(managerPhoneInfo);
        managerRequest.setEmploymentInfoList(Arrays.asList(managerEmploymentInfo));

        try {
            MvcResult result = mockMvc.perform(post("/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(APP_ORG_UUID, "org-1")
                            .header(APP_USER_UUID, "user-1")
                            .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                            .header(APP_TRACE_ID, "trace-1")
                            .header(APP_REGION_ID, "region-1")
                            .content(objectMapper.writeValueAsString(managerRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            JsonNode jsonNode = objectMapper.readTree(responseContent);

            if (jsonNode.get("result").get("httpStatus").asText().equals("BAD_REQUEST")) {
                result = mockMvc.perform(get("/user/manager-1")
                                .header(APP_ORG_UUID, "org-1")
                                .header(APP_USER_UUID, "user-1")
                                .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                                .header(APP_TRACE_ID, "trace-1")
                                .header(APP_REGION_ID, "region-1"))
                        .andExpect(status().isOk())
                        .andReturn();

                responseContent = result.getResponse().getContentAsString();
                jsonNode = objectMapper.readTree(responseContent);
                managerUuid = jsonNode.get("userId").asText();
            } else {
                managerUuid = jsonNode.get("result").get("userId").asText();
            }

            result = mockMvc.perform(get("/user/{userId}", managerUuid)
                            .header(APP_ORG_UUID, "org-1")
                            .header(APP_USER_UUID, "user-1")
                            .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                            .header(APP_TRACE_ID, "trace-1")
                            .header(APP_REGION_ID, "region-1"))
                    .andExpect(status().isOk())
                    .andReturn();

            responseContent = result.getResponse().getContentAsString();
            jsonNode = objectMapper.readTree(responseContent);

            Thread.sleep(1000);

            result = mockMvc.perform(get("/user/{userId}", managerUuid)
                            .header(APP_ORG_UUID, "org-1")
                            .header(APP_USER_UUID, "user-1")
                            .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                            .header(APP_TRACE_ID, "trace-1")
                            .header(APP_REGION_ID, "region-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(managerUuid))
                    .andExpect(jsonPath("$.jobProfiles").isArray())
                    .andReturn();

        } catch (Exception e) {
            throw new RuntimeException("Failed to setup test data - could not create or get manager user", e);
        }

        if (managerUuid == null) {
            throw new RuntimeException("Failed to setup test data - could not get manager UUID");
        }

        EmailInfo testEmailInfo = new EmailInfo();
        testEmailInfo.setEmail("test-" + UUID.randomUUID() + "@example.com");
        testEmailInfo.setVerificationStatus("VERIFIED");

        PhoneInfo testPhoneInfo = new PhoneInfo();
        testPhoneInfo.setNumber(UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 10));
        testPhoneInfo.setCountryCode(1);
        testPhoneInfo.setVerificationStatus("VERIFIED");

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
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.result.httpStatus").value("CREATED"))
                .andExpect(jsonPath("$.result.userId").exists())
                .andExpect(jsonPath("$.result.username").value(userId))
                .andExpect(jsonPath("$.result.status").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Transactional
    void createUser_WhenDuplicateUser_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.httpStatus").value("CREATED"));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.result.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("found existing users matching attributes: [phone, email, username]"));
    }

    @Test
    @Transactional
    void getUser_WhenUserExists_ShouldReturnUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.result.httpStatus").value("CREATED"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String actualUserId = jsonNode.get("result").get("userId").asText();

        mockMvc.perform(get("/user/{userId}", actualUserId)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(actualUserId))
                .andExpect(jsonPath("$.username").value(userId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value(createUserRequest.getEmailInfo().getEmail()))
                .andExpect(jsonPath("$.phone").value(createUserRequest.getPhoneInfo().getNumber()));
    }

    @Test
    @Transactional
    void getUser_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/user/{userId}", "non-existent-id")
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatus").value("NOT_FOUND"))
                .andExpect(jsonPath("$.userId").isEmpty())
                .andExpect(jsonPath("$.username").isEmpty())
                .andExpect(jsonPath("$.firstName").isEmpty())
                .andExpect(jsonPath("$.middleName").isEmpty())
                .andExpect(jsonPath("$.lastName").isEmpty())
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.phone").isEmpty())
                .andExpect(jsonPath("$.startDate").isEmpty())
                .andExpect(jsonPath("$.endDate").isEmpty())
                .andExpect(jsonPath("$.status").isEmpty())
                .andExpect(jsonPath("$.jobProfiles").isEmpty());
    }

    @Test
    void updateUser_WhenValidRequest_ShouldUpdateUser() throws Exception {
        String uniqueManagerUsername = "manager-" + UUID.randomUUID();
        String uniqueManagerEmail = "manager-" + UUID.randomUUID() + "@example.com";
        String uniqueManagerPhone = String.valueOf(System.currentTimeMillis()).substring(0, 10);

        EmailInfo managerEmailInfo = new EmailInfo();
        managerEmailInfo.setEmail(uniqueManagerEmail);
        managerEmailInfo.setVerificationStatus("VERIFIED");

        PhoneInfo managerPhoneInfo = new PhoneInfo();
        managerPhoneInfo.setNumber(uniqueManagerPhone);
        managerPhoneInfo.setCountryCode(1);
        managerPhoneInfo.setVerificationStatus("VERIFIED");

        EmploymentInfo managerEmploymentInfo = new EmploymentInfo();
        managerEmploymentInfo.setJobTitle("Manager");
        managerEmploymentInfo.setOrganizationUnit("Management");
        managerEmploymentInfo.setStartDate(LocalDateTime.now());
        managerEmploymentInfo.setReportingManager("");
        managerEmploymentInfo.setExtensionsData(new HashMap<>());
        managerEmploymentInfo.setEndDate(null);

        CreateUserRequest managerRequest = new CreateUserRequest();
        managerRequest.setUsername(uniqueManagerUsername);
        managerRequest.setFirstName("Manager");
        managerRequest.setLastName("One");
        managerRequest.setEmailInfo(managerEmailInfo);
        managerRequest.setPhoneInfo(managerPhoneInfo);
        managerRequest.setEmploymentInfoList(Arrays.asList(managerEmploymentInfo));

        MvcResult managerCreateResult = mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(managerRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String managerCreateResponse = managerCreateResult.getResponse().getContentAsString();
        JsonNode managerCreateJson = objectMapper.readTree(managerCreateResponse);
        String managerUuid = managerCreateJson.get("result").get("userId").asText();

        EmailInfo testEmailInfo = new EmailInfo();
        testEmailInfo.setEmail("test-" + UUID.randomUUID() + "@example.com");
        testEmailInfo.setVerificationStatus("VERIFIED");

        PhoneInfo testPhoneInfo = new PhoneInfo();
        testPhoneInfo.setNumber(UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 10));
        testPhoneInfo.setCountryCode(1);
        testPhoneInfo.setVerificationStatus("VERIFIED");

        EmploymentInfo employmentInfo = new EmploymentInfo();
        employmentInfo.setJobTitle("Software Engineer");
        employmentInfo.setOrganizationUnit("Engineering");
        employmentInfo.setStartDate(LocalDateTime.now());
        employmentInfo.setReportingManager(managerUuid);
        employmentInfo.setExtensionsData(new HashMap<>());
        employmentInfo.setEndDate(null);

        CreateUserRequest testUserRequest = new CreateUserRequest();
        testUserRequest.setUsername(UUID.randomUUID().toString());
        testUserRequest.setFirstName("John");
        testUserRequest.setLastName("Doe");
        testUserRequest.setEmailInfo(testEmailInfo);
        testUserRequest.setPhoneInfo(testPhoneInfo);
        testUserRequest.setEmploymentInfoList(Arrays.asList(employmentInfo));

        MvcResult result = mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(testUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.result.httpStatus").value("CREATED"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String actualUserId = jsonNode.get("result").get("userId").asText();

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setUsername(testUserRequest.getUsername());
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setEmailInfo(testUserRequest.getEmailInfo());
        updateRequest.setPhoneInfo(testUserRequest.getPhoneInfo());

        EmploymentInfo updateEmploymentInfo = new EmploymentInfo();
        updateEmploymentInfo.setJobTitle("Software Engineer");
        updateEmploymentInfo.setOrganizationUnit("Engineering");
        updateEmploymentInfo.setStartDate(LocalDateTime.now());
        updateEmploymentInfo.setReportingManager(managerUuid);
        updateEmploymentInfo.setExtensionsData(new HashMap<>());
        updateRequest.setEmploymentInfo(updateEmploymentInfo);

        mockMvc.perform(put("/user/{userId}", actualUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.status").value("Active"))
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/user/{userId}", actualUserId)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    @Transactional
    void deleteUser_WhenUserExists_ShouldDeleteUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String actualUserId = jsonNode.get("result").get("userId").asText();

        mockMvc.perform(delete("/user/{userId}", actualUserId)
                        .header(APP_ORG_UUID, "org-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value("Inactive"));

        mockMvc.perform(get("/user/{userId}", actualUserId)
                        .header(APP_ORG_UUID, "org-1")
                        .header(APP_USER_UUID, "user-1")
                        .header(APP_CLIENT_USER_SESSION_UUID, "session-1")
                        .header(APP_TRACE_ID, "trace-1")
                        .header(APP_REGION_ID, "region-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Inactive"));
    }
}