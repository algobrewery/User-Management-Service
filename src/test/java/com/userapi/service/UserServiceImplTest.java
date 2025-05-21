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

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}
