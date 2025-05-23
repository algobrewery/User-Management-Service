package com.userapi;

import com.userapi.models.entity.VerificationStatus;
import com.userapi.models.internal.EmailInfoDto;
import com.userapi.models.internal.PhoneInfoDto;
import com.userapi.models.internal.RequestContext;
import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.ArrayList;

/**
 * Constants for use in tests
 */
public class TestConstants {
    // Header constants
    public static final String TEST_USER_UUID = "user1";
    public static final String TEST_ORG_UUID = "org1";
    public static final String TEST_SESSION_UUID = "sess1";
    public static final String TEST_TRACE_ID = "trace1";
    public static final String TEST_REGION_ID = "region1";

    // Header constants for UserListControllerTest
    public static final String LIST_TEST_ORG_UUID = "org-uuid";
    public static final String LIST_TEST_USER_UUID = "user-uuid";
    public static final String LIST_TEST_SESSION_UUID = "session-uuid";
    public static final String LIST_TEST_TRACE_ID = "trace-id";
    public static final String LIST_TEST_REGION_ID = "region-id";

    // User data constants
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_FIRST_NAME = "First";
    public static final String TEST_LAST_NAME = "Last";
    public static final String TEST_USER_ID = "abc123";
    public static final String TEST_EMAIL = "user@example.com";
    public static final String TEST_PHONE = "1234567890";
    public static final int TEST_COUNTRY_CODE = 1;

    // Status constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    // Verification status constants
    public static final VerificationStatus VERIFICATION_STATUS_VERIFIED = VerificationStatus.VERIFIED;
    public static final VerificationStatus VERIFICATION_STATUS_UNVERIFIED = VerificationStatus.UNVERIFIED;
    public static final VerificationStatus VERIFICATION_STATUS_PENDING = VerificationStatus.PENDING;

    // Response constants
    public static final ResponseResult SUCCESS_RESULT = ResponseResult.SUCCESS;
    public static final ResponseReasonCode SUCCESS_REASON = ResponseReasonCode.SUCCESS;
    public static final ResponseResult FAILURE_RESULT = ResponseResult.FAILURE;
    public static final ResponseReasonCode ERROR_REASON = ResponseReasonCode.INTERNAL_SERVER_ERROR;
    public static final ResponseReasonCode NOT_FOUND_REASON = ResponseReasonCode.ENTITY_NOT_FOUND;
    public static final ResponseReasonCode DUPLICATE_REASON = ResponseReasonCode.DUPLICATE_USER;

    // HTTP Status constants
    public static final HttpStatus CREATED_STATUS = HttpStatus.CREATED;
    public static final HttpStatus OK_STATUS = HttpStatus.OK;
    public static final HttpStatus ERROR_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;
    public static final HttpStatus BAD_REQUEST_STATUS = HttpStatus.BAD_REQUEST;
    public static final HttpStatus NOT_FOUND_STATUS = HttpStatus.NOT_FOUND;

    // Content type constants
    public static final MediaType JSON_CONTENT_TYPE = MediaType.APPLICATION_JSON;

    // Success messages
    public static final String SUCCESS_CREATE_MESSAGE = "User created";
    public static final String SUCCESS_UPDATE_MESSAGE = "User updated";
    public static final String SUCCESS_DEACTIVATE_MESSAGE = "User deactivated";

    // Error messages
    public static final String GENERIC_ERROR_MESSAGE = "An error occurred";
    public static final String NOT_FOUND_MESSAGE = "User not found";
    public static final String DUPLICATE_MESSAGE = "User already exists";
    public static final String MISSING_HEADERS_MESSAGE = "Missing required headers";
    public static final String NULL_ORG_USER_MESSAGE = "Org UUID and User ID cannot be null";

    // Exception messages
    public static final String NULL_ORG_UUID_MESSAGE = "Null orgUUID";
    public static final String NULL_USER_UUID_MESSAGE = "Null userUUID";
    public static final String EXPECTED_EXCEPTION_MESSAGE = "Expected exception was not thrown";

    // Test data constants
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;

    // Common objects
    public static final RequestContext TEST_REQUEST_CONTEXT = RequestContext.builder()
            .appUserUuid(TEST_USER_UUID)
            .appOrgUuid(TEST_ORG_UUID)
            .appClientUserSessionUuid(TEST_SESSION_UUID)
            .traceId(TEST_TRACE_ID)
            .regionId(TEST_REGION_ID)
            .build();

    public static final PhoneInfoDto TEST_PHONE_INFO = PhoneInfoDto.builder()
            .number(TEST_PHONE)
            .countryCode(TEST_COUNTRY_CODE)
            .verificationStatus(VERIFICATION_STATUS_VERIFIED.toString())
            .build();

    public static final EmailInfoDto TEST_EMAIL_INFO = EmailInfoDto.builder()
            .email(TEST_EMAIL)
            .verificationStatus(VERIFICATION_STATUS_VERIFIED.toString())
            .build();

    public static final ArrayList<Object> EMPTY_LIST = new ArrayList<>();
}
