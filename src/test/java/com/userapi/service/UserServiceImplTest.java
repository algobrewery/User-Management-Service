package com.userapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.converters.EmploymentInfoDtoToJobProfileConverter;
import com.userapi.exception.*;
import com.userapi.models.entity.*;
import com.userapi.models.internal.*;
import com.userapi.repository.jobprofile.JobProfileRepository;
import com.userapi.repository.userprofile.UserProfileRepository;
import com.userapi.repository.userreportee.UserReporteeRepository;
import com.userapi.service.impl.UserServiceImpl;
import com.userapi.service.tasks.ReportingManagerFetcher;
import com.userapi.service.tasks.UpdateUserInternalRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private JobProfileRepository jobProfileRepository;
    @Mock
    private UserReporteeRepository userReporteeRepository;
    @Mock
    private EmploymentInfoDtoToJobProfileConverter employmentInfoDtoToJobProfileConverter;
    @Mock
    private ReportingManagerFetcher reportingManagerFetcher;
    @Mock
    private UpdateUserInternalRequestValidator updateUserInternalRequestValidator;
    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createUser_success() {
        CreateUserInternalRequest request = CreateUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid("org1")
                        .appUserUuid("user1")
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .username("user1")
                .firstName("John")
                .lastName("Doe")
                .emailInfo(EmailInfoDto.builder().email("user@example.com").verificationStatus("Verified").build())
                .phoneInfo(PhoneInfoDto.builder().number("1234567890").countryCode(1).verificationStatus("Verified").build())
                .employmentInfoList(Collections.emptyList())
                .build();

        when(userProfileRepository.findUsersMatchingAny(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reportingManagerFetcher.fetchMatchingJobProfileUuids(anyString(), anyList()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

        CompletableFuture<CreateUserInternalResponse> responseFuture = userService.createUser(request);

        CreateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.SUCCESS, response.getResponseResult());
        assertEquals(ResponseReasonCode.SUCCESS, response.getResponseReasonCode());
        assertEquals("User created successfully", response.getMessage());
    }

    @Test
    void createUser_duplicateUser() {
        CreateUserInternalRequest request = CreateUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid("org1")
                        .appUserUuid("user1")
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .username("user1")
                .firstName("John")
                .lastName("Doe")
                .emailInfo(EmailInfoDto.builder().email("user@example.com").verificationStatus("Verified").build())
                .phoneInfo(PhoneInfoDto.builder().number("1234567890").countryCode(1).verificationStatus("Verified").build())
                .employmentInfoList(Collections.emptyList())
                .build();

        UserProfile existingUser = UserProfile.builder()
                .username("user1")
                .email("user@example.com")
                .phone("1234567890")
                .build();

        when(userProfileRepository.findUsersMatchingAny(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.singletonList(existingUser));
        when(reportingManagerFetcher.fetchMatchingJobProfileUuids(anyString(), anyList()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));

        CompletableFuture<CreateUserInternalResponse> responseFuture = userService.createUser(request);

        CreateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.DUPLICATE_USER, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("found existing users matching attributes"));
    }

    @Test
    void getUser_success() {
        String orgUuid = "org1";
        String userId = "user1";

        UserProfile userProfile = UserProfile.builder()
                .userUuid(userId)
                .organizationUuid(orgUuid)
                .username("user1")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .phoneCountryCode(1)
                .jobProfileUuids(new String[]{"job-profile-1"})
                .build();

        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(userProfile);

        JobProfile jobProfile = JobProfile.builder()
                .jobProfileUuid("job-profile-1")
                .organizationUuid(orgUuid)
                .title("Software Engineer")
                .build();

        when(jobProfileRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Collections.singletonList(jobProfile));

        CompletableFuture<GetUserInternalResponse> responseFuture = userService.getUser(
                GetUserInternalRequest.builder()
                        .requestContext(RequestContext.builder()
                                .appOrgUuid(orgUuid)
                                .appUserUuid("user1")
                                .appClientUserSessionUuid("sess1")
                                .traceId("trace1")
                                .regionId("region1")
                                .build())
                        .userId(userId)
                        .build());

        GetUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.SUCCESS, response.getResponseResult());
        assertEquals(ResponseReasonCode.SUCCESS, response.getResponseReasonCode());
        assertEquals("fetched userprofile successfully", response.getMessage());
    }

    @Test
    void getUser_notFound() {
        String orgUuid = "org1";
        String userId = "user1";

        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(null);

        CompletableFuture<GetUserInternalResponse> responseFuture = userService.getUser(
                GetUserInternalRequest.builder()
                        .requestContext(RequestContext.builder()
                                .appOrgUuid(orgUuid)
                                .appUserUuid("user1")
                                .appClientUserSessionUuid("sess1")
                                .traceId("trace1")
                                .regionId("region1")
                                .build())
                        .userId(userId)
                        .build());

        GetUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.ENTITY_NOT_FOUND, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("User not found"));
    }

    @Test
    void deactivateUser_success() {
        String orgUuid = "org1";
        String userId = "user1";

        UserProfile userProfile = UserProfile.builder()
                .userUuid(userId)
                .organizationUuid(orgUuid)
                .status(UserStatus.ACTIVE.getName())
                .build();

        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(userProfile);
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<UpdateUserInternalResponse> responseFuture = userService.deactivateUser(orgUuid, userId);

        UpdateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.SUCCESS, response.getResponseResult());
        assertEquals(ResponseReasonCode.SUCCESS, response.getResponseReasonCode());
        assertEquals("Updated user successfully", response.getMessage());
        assertEquals(UserStatus.INACTIVE.getName(), response.getStatus());
    }

    @Test
    void deactivateUser_notFound() {
        String orgUuid = "org1";
        String userId = "user1";

        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(null);

        CompletableFuture<UpdateUserInternalResponse> responseFuture = userService.deactivateUser(orgUuid, userId);

        UpdateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.ENTITY_NOT_FOUND, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("User not found"));
    }

    @Test
    void createUser_managerNotFound() {
        // Arrange
        String orgUuid = "org1";
        String managerUuid = "manager1";

        EmploymentInfoDto employmentInfo = EmploymentInfoDto.builder()
                .jobTitle("Developer")
                .organizationUnit("Engineering")
                .startDate(LocalDateTime.now())
                .reportingManager(managerUuid)
                .extensionsData(Map.of("key", "value"))
                .build();

        List<EmploymentInfoDto> employmentInfoList = Collections.singletonList(employmentInfo);

        CreateUserInternalRequest request = CreateUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid(orgUuid)
                        .appUserUuid("user1")
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .username("user1")
                .firstName("John")
                .lastName("Doe")
                .emailInfo(EmailInfoDto.builder().email("user@example.com").verificationStatus("Verified").build())
                .phoneInfo(PhoneInfoDto.builder().number("1234567890").countryCode(1).verificationStatus("Verified").build())
                .employmentInfoList(employmentInfoList)
                .build();

        // No existing users with matching attributes
        when(userProfileRepository.findUsersMatchingAny(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        // Manager not found - simulate ResourceNotFoundException
        CompletableFuture<Map<EmploymentInfoDto, List<JobProfile>>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new ResourceNotFoundException("Unable to find reportingManagers:" + managerUuid));
        when(reportingManagerFetcher.fetchMatchingJobProfileUuids(eq(orgUuid), anyList()))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<CreateUserInternalResponse> responseFuture = userService.createUser(request);

        // Assert
        CreateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.ENTITY_NOT_FOUND, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("Resource not found"));
        assertTrue(response.getMessage().contains(managerUuid));

        // Verify that save was never called since we failed early
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void createUser_jobProfileCreationError() {
        // Arrange
        String orgUuid = "org1";
        String userUuid = "user1";
        String managerUuid = "manager1";

        EmploymentInfoDto employmentInfo = EmploymentInfoDto.builder()
                .jobTitle("Developer")
                .organizationUnit("Engineering")
                .startDate(LocalDateTime.now())
                .reportingManager(managerUuid)
                .extensionsData(Map.of("key", "value"))
                .build();

        List<EmploymentInfoDto> employmentInfoList = Collections.singletonList(employmentInfo);

        CreateUserInternalRequest request = CreateUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid(orgUuid)
                        .appUserUuid(userUuid)
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .username("user1")
                .firstName("John")
                .lastName("Doe")
                .emailInfo(EmailInfoDto.builder().email("user@example.com").verificationStatus("Verified").build())
                .phoneInfo(PhoneInfoDto.builder().number("1234567890").countryCode(1).verificationStatus("Verified").build())
                .employmentInfoList(employmentInfoList)
                .build();

        // No existing users with matching attributes
        when(userProfileRepository.findUsersMatchingAny(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        // Manager found but empty job profiles
        Map<EmploymentInfoDto, List<JobProfile>> emptyJobProfilesMap = new HashMap<>();
        emptyJobProfilesMap.put(employmentInfo, Collections.emptyList());
        when(reportingManagerFetcher.fetchMatchingJobProfileUuids(eq(orgUuid), anyList()))
                .thenReturn(CompletableFuture.completedFuture(emptyJobProfilesMap));

        // Job profile creation fails
        when(employmentInfoDtoToJobProfileConverter.convert(any(EmploymentInfoDto.class), anyString()))
                .thenThrow(new RuntimeException("Error creating job profile"));

        // Act
        CompletableFuture<CreateUserInternalResponse> responseFuture = userService.createUser(request);

        // Assert
        CreateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.INTERNAL_SERVER_ERROR, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("Internal server error"));
        assertTrue(response.getMessage().contains("Error creating job profile"));

        // Verify that save was never called since we failed early
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void updateUser_withInvalidData() {
        // Arrange
        String orgUuid = "org1";
        String userId = "user1";

        // Existing user
        UserProfile existingUser = UserProfile.builder()
                .userUuid(userId)
                .organizationUuid(orgUuid)
                .username("oldUsername")
                .firstName("Old")
                .lastName("Name")
                .email("old@example.com")
                .phone("9876543210")
                .phoneCountryCode(1)
                .status(UserStatus.ACTIVE.getName())
                .jobProfileUuids(new String[]{"job1"})
                .build();

        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(existingUser);

        // Update request with invalid data
        UpdateUserInternalRequest request = UpdateUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid(orgUuid)
                        .appUserUuid("user2")
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .username("newUsername")
                .firstName("New")
                .lastName("Name")
                .emailInfo(EmailInfoDto.builder().email("new@example.com").verificationStatus("Verified").build())
                .phoneInfo(PhoneInfoDto.builder().number("1234567890").countryCode(1).verificationStatus("Verified").build())
                .build();

        // Validation fails
        CompletableFuture<UpdateUserInternalRequest> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalArgumentException("Invalid data: Email format is incorrect"));
        when(updateUserInternalRequestValidator.validateUniqueUser(eq(existingUser), eq(request)))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<UpdateUserInternalResponse> responseFuture = userService.updateUser(userId, request);

        // Assert
        UpdateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.INTERNAL_SERVER_ERROR, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("Internal server error"));
        assertTrue(response.getMessage().contains("Invalid data"));

        // Verify that save was never called
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void updateUser_withDuplicateData() {
        // Arrange
        String orgUuid = "org1";
        String userId = "user1";

        // Existing user
        UserProfile existingUser = UserProfile.builder()
                .userUuid(userId)
                .organizationUuid(orgUuid)
                .username("oldUsername")
                .firstName("Old")
                .lastName("Name")
                .email("old@example.com")
                .phone("9876543210")
                .phoneCountryCode(1)
                .status(UserStatus.ACTIVE.getName())
                .jobProfileUuids(new String[]{"job1"})
                .build();

        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(existingUser);

        // Update request with duplicate data
        UpdateUserInternalRequest request = UpdateUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid(orgUuid)
                        .appUserUuid("user2")
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .username("existingUsername")  // This username already exists for another user
                .firstName("New")
                .lastName("Name")
                .emailInfo(EmailInfoDto.builder().email("existing@example.com").verificationStatus("Verified").build())
                .phoneInfo(PhoneInfoDto.builder().number("1234567890").countryCode(1).verificationStatus("Verified").build())
                .build();

        // Validation fails with duplicate error
        CompletableFuture<UpdateUserInternalRequest> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new DuplicateResourceException("Username already exists"));
        when(updateUserInternalRequestValidator.validateUniqueUser(eq(existingUser), eq(request)))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<UpdateUserInternalResponse> responseFuture = userService.updateUser(userId, request);

        // Assert
        UpdateUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.INTERNAL_SERVER_ERROR, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("Internal server error"));
        assertTrue(response.getMessage().contains("Username already exists"));

        // Verify that save was never called
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void getUserHierarchy_userNotFound() {
        // Arrange
        String orgUuid = "org1";
        String userId = "user1";

        // User not found
        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenReturn(null);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserHierarchy(orgUuid, userId);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        assertTrue(exception.getMessage().contains(userId));
    }

    @Test
    void getUser_internalServerError() {
        // Arrange
        String orgUuid = "org1";
        String userId = "user1";

        // Simulate database error
        when(userProfileRepository.findByUserId(orgUuid, userId))
                .thenThrow(new RuntimeException("Database connection error"));

        GetUserInternalRequest request = GetUserInternalRequest.builder()
                .requestContext(RequestContext.builder()
                        .appOrgUuid(orgUuid)
                        .appUserUuid("user2")
                        .appClientUserSessionUuid("sess1")
                        .traceId("trace1")
                        .regionId("region1")
                        .build())
                .userId(userId)
                .build();

        // Act
        CompletableFuture<GetUserInternalResponse> responseFuture = userService.getUser(request);

        // Assert
        GetUserInternalResponse response = responseFuture.join();
        assertEquals(ResponseResult.FAILURE, response.getResponseResult());
        assertEquals(ResponseReasonCode.INTERNAL_SERVER_ERROR, response.getResponseReasonCode());
        assertTrue(response.getMessage().contains("Internal server error"));
        assertTrue(response.getMessage().contains("Database connection error"));
    }
}
