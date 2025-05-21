package com.userapi.controller;

import com.userapi.converters.*;
import com.userapi.models.external.*;
import com.userapi.models.internal.*;
import com.userapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private CreateUserRequestConverter createUserRequestConverter;
    @Mock
    private CreateUserResponseConverter createUserResponseConverter;
    @Mock
    private GetUserRequestConverter getUserRequestConverter;
    @Mock
    private GetUserResponseConverter getUserResponseConverter;
    @Mock
    private UpdateUserRequestConverter updateUserRequestConverter;
    @Mock
    private UpdateUserResponseConverter updateUserResponseConverter;
    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createUser_success() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        RequestContext requestContext = RequestContext.builder()
                .appUserUuid("user1")
                .appOrgUuid("org1")
                .appClientUserSessionUuid("sess1")
                .traceId("trace1")
                .regionId("region1")
                .build();
        PhoneInfoDto phoneInfo = PhoneInfoDto.builder()
                .number("1234567890")
                .countryCode(1)
                .verificationStatus("Verified")
                .build();
        EmailInfoDto emailInfo = EmailInfoDto.builder()
                .email("user@example.com")
                .verificationStatus("Verified")
                .build();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(requestContext)
                .username("user1")
                .firstName("First")
                .lastName("Last")
                .phoneInfo(phoneInfo)
                .emailInfo(emailInfo)
                .employmentInfoList(new ArrayList<>())
                .build();
        CreateUserInternalResponse internalResponse = CreateUserInternalResponse.builder()
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .build();
        CreateUserResponse expectedResponse = CreateUserResponse.builder().message("User created").httpStatus(HttpStatus.CREATED).build();

        when(createUserRequestConverter.toInternal(anyString(), anyString(), anyString(), anyString(), anyString(), eq(request)))
                .thenReturn(internalRequest);

        when(userService.createUser(internalRequest))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(createUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expectedResponse);

        ResponseEntity<CreateUserResponse> response = userController.createUser("org1", "user1", "sess1", "trace1", "region1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User created", response.getBody().getMessage());
    }

    @Test
    void getUserById_success() throws Exception {
        String userId = "abc123";
        RequestContext requestContext = RequestContext.builder()
                .appUserUuid("user1")
                .appOrgUuid("org1")
                .appClientUserSessionUuid("sess1")
                .traceId("trace1")
                .regionId("region1")
                .build();
        GetUserInternalRequest internalRequest = GetUserInternalRequest.builder()
                .requestContext(requestContext)
                .userId(userId)
                .build();
        GetUserInternalResponse internalResponse = GetUserInternalResponse.builder()
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .build();
        GetUserResponse expected = GetUserResponse.builder().httpStatus(HttpStatus.OK).build();

        when(getUserRequestConverter.toInternal(anyString(), anyString(), anyString(), anyString(), anyString(), eq(userId)))
                .thenReturn(internalRequest);

        when(userService.getUser(internalRequest))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(getUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expected);

        ResponseEntity<GetUserResponse> response = userController.getUserById("org1", "user1", "sess1", "trace1", "region1", userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateUser_success() throws Exception {
        String userId = "abc123";
        UpdateUserRequest request = new UpdateUserRequest();
        RequestContext requestContext = RequestContext.builder()
                .appUserUuid("user1")
                .appOrgUuid("org1")
                .appClientUserSessionUuid("sess1")
                .traceId("trace1")
                .regionId("region1")
                .build();
        UpdateUserInternalRequest internalRequest = UpdateUserInternalRequest.builder()
                .requestContext(requestContext)
                .build();
        UpdateUserInternalResponse internalResponse = UpdateUserInternalResponse.builder()
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .build();
        UpdateUserResponse expected = UpdateUserResponse.builder().httpStatus(HttpStatus.OK).build();

        when(updateUserRequestConverter.toInternal(anyString(), anyString(), anyString(), anyString(), anyString(), eq(request)))
                .thenReturn(internalRequest);

        when(userService.updateUser(eq(userId), eq(internalRequest)))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(updateUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expected);

        ResponseEntity<UpdateUserResponse> response = userController.updateUser("org1", "user1", "sess1", "trace1", "region1", userId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deactivateUser_success() throws Exception {
        String userId = "abc123";
        UpdateUserInternalResponse internalResponse = UpdateUserInternalResponse.builder()
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .build();
        UpdateUserResponse expected = UpdateUserResponse.builder().httpStatus(HttpStatus.OK).build();

        when(userService.deactivateUser("org1", userId))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(updateUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expected);

        ResponseEntity<UpdateUserResponse> response = userController.deactivateUser("org1", userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createUser_exception() {
        CreateUserRequest request = new CreateUserRequest();
        RequestContext requestContext = RequestContext.builder()
                .appUserUuid("user1")
                .appOrgUuid("org1")
                .appClientUserSessionUuid("sess1")
                .traceId("trace1")
                .regionId("region1")
                .build();
        PhoneInfoDto phoneInfo = PhoneInfoDto.builder()
                .number("1234567890")
                .countryCode(1)
                .verificationStatus("Verified")
                .build();
        EmailInfoDto emailInfo = EmailInfoDto.builder()
                .email("user@example.com")
                .verificationStatus("Verified")
                .build();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(requestContext)
                .username("user1")
                .firstName("First")
                .lastName("Last")
                .phoneInfo(phoneInfo)
                .emailInfo(emailInfo)
                .employmentInfoList(new ArrayList<>())
                .build();

        when(createUserRequestConverter.toInternal(anyString(), anyString(), anyString(), anyString(), anyString(), eq(request)))
                .thenReturn(internalRequest);

        CompletableFuture<CreateUserInternalResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed"));
        when(userService.createUser(internalRequest))
                .thenReturn(failedFuture);

        ResponseEntity<CreateUserResponse> response = userController.createUser("org1", "user1", "sess1", "trace1", "region1", request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("java.lang.RuntimeException: Failed", response.getBody().getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getBody().getHttpStatus());
    }
}
