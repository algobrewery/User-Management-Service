package com.userapi.controller;

import com.userapi.TestConstants;
import com.userapi.converters.CreateUserRequestConverter;
import com.userapi.converters.CreateUserResponseConverter;
import com.userapi.converters.GetUserRequestConverter;
import com.userapi.converters.GetUserResponseConverter;
import com.userapi.converters.UpdateUserRequestConverter;
import com.userapi.converters.UpdateUserResponseConverter;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.exception.ResourceNotFoundException;
import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.external.GetUserResponse;
import com.userapi.models.external.UpdateUserRequest;
import com.userapi.models.external.UpdateUserResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.internal.EmailInfoDto;
import com.userapi.models.internal.GetUserInternalRequest;
import com.userapi.models.internal.GetUserInternalResponse;
import com.userapi.models.internal.PhoneInfoDto;
import com.userapi.models.internal.RequestContext;
import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import com.userapi.models.internal.UpdateUserInternalRequest;
import com.userapi.models.internal.UpdateUserInternalResponse;
import com.userapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.userapi.TestConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .phoneInfo(TEST_PHONE_INFO)
                .emailInfo(TEST_EMAIL_INFO)
                .employmentInfoList(new ArrayList<>())
                .build();

        CreateUserInternalResponse internalResponse = CreateUserInternalResponse.builder()
                .responseResult(SUCCESS_RESULT)
                .responseReasonCode(SUCCESS_REASON)
                .userId(TEST_USER_ID)
                .username(TEST_USERNAME)
                .status(STATUS_ACTIVE)
                .message(SUCCESS_CREATE_MESSAGE)
                .build();

        CreateUserResponse expectedResponse = CreateUserResponse.builder()
                .message(SUCCESS_CREATE_MESSAGE)
                .httpStatus(CREATED_STATUS)
                .userId(TEST_USER_ID)
                .username(TEST_USERNAME)
                .status(STATUS_ACTIVE)
                .build();

        when(createUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenReturn(internalRequest);

        when(userService.createUser(internalRequest))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(createUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<CreateUserResponse> response = userController.createUser(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                request);

        // Assert
        assertEquals(CREATED_STATUS, response.getStatusCode());
        assertEquals(SUCCESS_CREATE_MESSAGE, response.getBody().getMessage());
        assertEquals(TEST_USER_ID, response.getBody().getUserId());
        assertEquals(TEST_USERNAME, response.getBody().getUsername());
        assertEquals(STATUS_ACTIVE, response.getBody().getStatus());
    }

    @Test
    void getUserById_success() throws Exception {
        // Arrange
        GetUserInternalRequest internalRequest = GetUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .userId(TEST_USER_ID)
                .build();

        GetUserInternalResponse internalResponse = GetUserInternalResponse.builder()
                .responseResult(SUCCESS_RESULT)
                .responseReasonCode(SUCCESS_REASON)
                .build();

        GetUserResponse expected = GetUserResponse.builder()
                .httpStatus(OK_STATUS)
                .userId(TEST_USER_ID)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .email(TEST_EMAIL)
                .phone(TEST_PHONE)
                .status(STATUS_ACTIVE)
                .build();

        when(getUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(TEST_USER_ID)))
                .thenReturn(internalRequest);

        when(userService.getUser(internalRequest))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(getUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expected);

        // Act
        ResponseEntity<GetUserResponse> response = userController.getUserById(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                TEST_USER_ID);

        // Assert
        assertEquals(OK_STATUS, response.getStatusCode());
        assertEquals(TEST_USER_ID, response.getBody().getUserId());
        assertEquals(TEST_USERNAME, response.getBody().getUsername());
        assertEquals(TEST_FIRST_NAME, response.getBody().getFirstName());
        assertEquals(TEST_LAST_NAME, response.getBody().getLastName());
        assertEquals(TEST_EMAIL, response.getBody().getEmail());
        assertEquals(TEST_PHONE, response.getBody().getPhone());
        assertEquals(STATUS_ACTIVE, response.getBody().getStatus());
    }

    @Test
    void updateUser_success() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        UpdateUserInternalRequest internalRequest = UpdateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .phoneInfo(TEST_PHONE_INFO)
                .emailInfo(TEST_EMAIL_INFO)
                .build();

        UpdateUserInternalResponse internalResponse = UpdateUserInternalResponse.builder()
                .responseResult(SUCCESS_RESULT)
                .responseReasonCode(SUCCESS_REASON)
                .userId(TEST_USER_ID)
                .status(STATUS_ACTIVE)
                .build();

        UpdateUserResponse expected = UpdateUserResponse.builder()
                .httpStatus(OK_STATUS)
                .userId(TEST_USER_ID)
                .status(STATUS_ACTIVE)
                .message(SUCCESS_UPDATE_MESSAGE)
                .build();

        when(updateUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenReturn(internalRequest);

        when(userService.updateUser(eq(TEST_USER_ID), eq(internalRequest)))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(updateUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expected);

        // Act
        ResponseEntity<UpdateUserResponse> response = userController.updateUser(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                TEST_USER_ID,
                request);

        // Assert
        assertEquals(OK_STATUS, response.getStatusCode());
        assertEquals(TEST_USER_ID, response.getBody().getUserId());
        assertEquals(STATUS_ACTIVE, response.getBody().getStatus());
        assertEquals(SUCCESS_UPDATE_MESSAGE, response.getBody().getMessage());
    }

    @Test
    void deactivateUser_success() throws Exception {
        // Arrange
        UpdateUserInternalResponse internalResponse = UpdateUserInternalResponse.builder()
                .responseResult(SUCCESS_RESULT)
                .responseReasonCode(SUCCESS_REASON)
                .userId(TEST_USER_ID)
                .status(STATUS_INACTIVE)
                .build();

        UpdateUserResponse expected = UpdateUserResponse.builder()
                .httpStatus(OK_STATUS)
                .userId(TEST_USER_ID)
                .status(STATUS_INACTIVE)
                .message(SUCCESS_DEACTIVATE_MESSAGE)
                .build();

        when(userService.deactivateUser(TEST_ORG_UUID, TEST_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(internalResponse));

        when(updateUserResponseConverter.toExternal(internalResponse))
                .thenReturn(expected);

        // Act
        ResponseEntity<UpdateUserResponse> response = userController.deactivateUser(TEST_ORG_UUID, TEST_USER_ID);

        // Assert
        assertEquals(OK_STATUS, response.getStatusCode());
        assertEquals(TEST_USER_ID, response.getBody().getUserId());
        assertEquals(STATUS_INACTIVE, response.getBody().getStatus());
        assertEquals(SUCCESS_DEACTIVATE_MESSAGE, response.getBody().getMessage());
    }

    @Test
    void createUser_serviceException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .phoneInfo(TEST_PHONE_INFO)
                .emailInfo(TEST_EMAIL_INFO)
                .employmentInfoList(new ArrayList<>())
                .build();

        when(createUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenReturn(internalRequest);

        CompletableFuture<CreateUserInternalResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed"));
        when(userService.createUser(internalRequest))
                .thenReturn(failedFuture);

        // Act
        ResponseEntity<CreateUserResponse> response = userController.createUser(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                request);

        // Assert
        assertEquals(ERROR_STATUS, response.getStatusCode());
        assertEquals("java.lang.RuntimeException: Failed", response.getBody().getMessage());
        assertEquals(ERROR_STATUS, response.getBody().getHttpStatus());
    }

    @Test
    void createUser_duplicateResourceException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .phoneInfo(TEST_PHONE_INFO)
                .emailInfo(TEST_EMAIL_INFO)
                .employmentInfoList(new ArrayList<>())
                .build();

        when(createUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenReturn(internalRequest);

        CompletableFuture<CreateUserInternalResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new DuplicateResourceException("User already exists"));
        when(userService.createUser(internalRequest))
                .thenReturn(failedFuture);

        // Act
        ResponseEntity<CreateUserResponse> response = userController.createUser(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                request);

        // Assert
        assertEquals(BAD_REQUEST_STATUS, response.getStatusCode());
        assertEquals("User already exists", response.getBody().getMessage());
        assertEquals(BAD_REQUEST_STATUS, response.getBody().getHttpStatus());
    }

    @Test
    void getUserById_serviceException() {
        // Arrange
        GetUserInternalRequest internalRequest = GetUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .userId(TEST_USER_ID)
                .build();

        when(getUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(TEST_USER_ID)))
                .thenReturn(internalRequest);

        CompletableFuture<GetUserInternalResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed"));
        when(userService.getUser(internalRequest))
                .thenReturn(failedFuture);

        // Act
        ResponseEntity<GetUserResponse> response = userController.getUserById(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                TEST_USER_ID);

        // Assert
        assertEquals(ERROR_STATUS, response.getStatusCode());
        assertEquals(ERROR_STATUS, response.getBody().getHttpStatus());
    }

    @Test
    void updateUser_serviceException() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        UpdateUserInternalRequest internalRequest = UpdateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .build();

        when(updateUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenReturn(internalRequest);

        CompletableFuture<UpdateUserInternalResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed"));
        when(userService.updateUser(eq(TEST_USER_ID), eq(internalRequest)))
                .thenReturn(failedFuture);

        // Act
        ResponseEntity<UpdateUserResponse> response = userController.updateUser(
                TEST_ORG_UUID,
                TEST_USER_UUID,
                TEST_SESSION_UUID,
                TEST_TRACE_ID,
                TEST_REGION_ID,
                TEST_USER_ID,
                request);

        // Assert
        assertEquals(ERROR_STATUS, response.getStatusCode());
        assertEquals(ERROR_STATUS, response.getBody().getHttpStatus());
    }

    @Test
    void deactivateUser_serviceException() {
        // Arrange
        CompletableFuture<UpdateUserInternalResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed"));
        when(userService.deactivateUser(TEST_ORG_UUID, TEST_USER_ID))
                .thenReturn(failedFuture);

        // Act
        ResponseEntity<UpdateUserResponse> response = userController.deactivateUser(TEST_ORG_UUID, TEST_USER_ID);

        // Assert
        assertEquals(ERROR_STATUS, response.getStatusCode());
        assertEquals(ERROR_STATUS, response.getBody().getHttpStatus());
    }

    @Test
    void createUser_missingOrgUuidHeader() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .phoneInfo(TEST_PHONE_INFO)
                .emailInfo(TEST_EMAIL_INFO)
                .employmentInfoList(new ArrayList<>())
                .build();

        when(createUserRequestConverter.toInternal(
                eq(null),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenThrow(new NullPointerException(NULL_ORG_UUID_MESSAGE));

        // Act & Assert
        try {
            userController.createUser(
                    null,
                    TEST_USER_UUID,
                    TEST_SESSION_UUID,
                    TEST_TRACE_ID,
                    TEST_REGION_ID,
                    request);
            // If we get here, the test should fail
            assertTrue(false, EXPECTED_EXCEPTION_MESSAGE);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    void createUser_missingUserUuidHeader() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        CreateUserInternalRequest internalRequest = CreateUserInternalRequest.builder()
                .requestContext(TEST_REQUEST_CONTEXT)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .phoneInfo(TEST_PHONE_INFO)
                .emailInfo(TEST_EMAIL_INFO)
                .employmentInfoList(new ArrayList<>())
                .build();

        when(createUserRequestConverter.toInternal(
                eq(TEST_ORG_UUID),
                eq(null),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(request)))
                .thenThrow(new NullPointerException(NULL_USER_UUID_MESSAGE));

        // Act & Assert
        try {
            userController.createUser(
                    TEST_ORG_UUID,
                    null,
                    TEST_SESSION_UUID,
                    TEST_TRACE_ID,
                    TEST_REGION_ID,
                    request);
            // If we get here, the test should fail
            assertTrue(false, EXPECTED_EXCEPTION_MESSAGE);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    void getUserById_missingHeaders() {
        // Arrange
        when(getUserRequestConverter.toInternal(
                eq(null),
                eq(TEST_USER_UUID),
                eq(TEST_SESSION_UUID),
                eq(TEST_TRACE_ID),
                eq(TEST_REGION_ID),
                eq(TEST_USER_ID)))
                .thenThrow(new NullPointerException(NULL_ORG_UUID_MESSAGE));

        // Act & Assert
        try {
            userController.getUserById(
                    null,
                    TEST_USER_UUID,
                    TEST_SESSION_UUID,
                    TEST_TRACE_ID,
                    TEST_REGION_ID,
                    TEST_USER_ID);
            // If we get here, the test should fail
            assertTrue(false, EXPECTED_EXCEPTION_MESSAGE);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    void updateUser_missingHeaders() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();

        // Act & Assert
        try {
            userController.updateUser(
                    null,
                    TEST_USER_UUID,
                    TEST_SESSION_UUID,
                    TEST_TRACE_ID,
                    TEST_REGION_ID,
                    TEST_USER_ID,
                    request);
            // If we get here, the test should fail
            assertTrue(false, EXPECTED_EXCEPTION_MESSAGE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals(MISSING_HEADERS_MESSAGE, e.getMessage());
        }
    }

    @Test
    void deactivateUser_missingOrgUuidHeader() {
        // Act
        try {
            userController.deactivateUser(null, TEST_USER_ID);
            // If we get here, the test should fail
            assertTrue(false, EXPECTED_EXCEPTION_MESSAGE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals(NULL_ORG_USER_MESSAGE, e.getMessage());
        }
    }

    @Test
    void deactivateUser_missingUserIdParam() {
        // Act
        try {
            userController.deactivateUser(TEST_ORG_UUID, null);
            // If we get here, the test should fail
            assertTrue(false, EXPECTED_EXCEPTION_MESSAGE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals(NULL_ORG_USER_MESSAGE, e.getMessage());
        }
    }
}