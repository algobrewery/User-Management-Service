package com.userapi.service.tasks;

import com.userapi.exception.DuplicateResourceException;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.internal.EmailInfoDto;
import com.userapi.models.internal.PhoneInfoDto;
import com.userapi.models.internal.RequestContext;
import com.userapi.models.internal.UpdateUserInternalRequest;
import com.userapi.repository.userprofile.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserInternalRequestValidatorTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UpdateUserInternalRequestValidator validator;

    private String orgUuid;
    private String userUuid;
    private String clientUserSessionUuid;
    private String traceId;
    private String regionId;

    @BeforeEach
    void setUp() {
        orgUuid = UUID.randomUUID().toString();
        userUuid = UUID.randomUUID().toString();
        clientUserSessionUuid = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        regionId = "US";
    }

    @Test
    void validateUniqueUser_WhenNoDuplicates_ShouldReturnRequest() {
        // Arrange
        UserProfile existingUser = new UserProfile();
        existingUser.setUserUuid(userUuid);
        existingUser.setOrganizationUuid(orgUuid);

        RequestContext requestContext = RequestContext.builder()
                .appOrgUuid(orgUuid)
                .appUserUuid(userUuid)
                .appClientUserSessionUuid(clientUserSessionUuid)
                .traceId(traceId)
                .regionId(regionId)
                .build();

        PhoneInfoDto phoneInfo = PhoneInfoDto.builder()
                .number("1234567890")
                .countryCode(1)
                .verificationStatus("VERIFIED")
                .build();

        EmailInfoDto emailInfo = EmailInfoDto.builder()
                .email("unique@example.com")
                .verificationStatus("VERIFIED")
                .build();

        UpdateUserInternalRequest request = UpdateUserInternalRequest.builder()
                .requestContext(requestContext)
                .phoneInfo(phoneInfo)
                .emailInfo(emailInfo)
                .build();

        when(userProfileRepository.findUserByPhoneNumber(orgUuid, "1234567890")).thenReturn(null);
        when(userProfileRepository.findUserByEmail(orgUuid, "unique@example.com")).thenReturn(null);

        // Act
        CompletableFuture<UpdateUserInternalRequest> result = validator.validateUniqueUser(existingUser, request);

        // Assert
        assertNotNull(result);
        UpdateUserInternalRequest validatedRequest = result.join();
        assertEquals(request, validatedRequest);
        verify(userProfileRepository).findUserByPhoneNumber(orgUuid, "1234567890");
        verify(userProfileRepository).findUserByEmail(orgUuid, "unique@example.com");
    }

    @Test
    void validateUniqueUser_WhenPhoneNumberExists_ShouldThrowException() {
        // Arrange
        UserProfile existingUser = new UserProfile();
        existingUser.setUserUuid(userUuid);
        existingUser.setOrganizationUuid(orgUuid);

        RequestContext requestContext = RequestContext.builder()
                .appOrgUuid(orgUuid)
                .appUserUuid(userUuid)
                .appClientUserSessionUuid(clientUserSessionUuid)
                .traceId(traceId)
                .regionId(regionId)
                .build();

        PhoneInfoDto phoneInfo = PhoneInfoDto.builder()
                .number("duplicatePhone")
                .countryCode(1)
                .verificationStatus("VERIFIED")
                .build();

        UpdateUserInternalRequest request = UpdateUserInternalRequest.builder()
                .requestContext(requestContext)
                .phoneInfo(phoneInfo)
                .build();

        UserProfile duplicateUser = new UserProfile();
        duplicateUser.setUserUuid(UUID.randomUUID().toString());
        when(userProfileRepository.findUserByPhoneNumber(orgUuid, "duplicatePhone")).thenReturn(duplicateUser);

        // Act & Assert
        CompletableFuture<UpdateUserInternalRequest> result = validator.validateUniqueUser(existingUser, request);
        CompletionException exception = assertThrows(CompletionException.class, () -> result.join());
        assertTrue(exception.getCause() instanceof DuplicateResourceException);
    }

    @Test
    void validateUniqueUser_WhenEmailExists_ShouldThrowException() {
        // Arrange
        UserProfile existingUser = new UserProfile();
        existingUser.setUserUuid(userUuid);
        existingUser.setOrganizationUuid(orgUuid);

        RequestContext requestContext = RequestContext.builder()
                .appOrgUuid(orgUuid)
                .appUserUuid(userUuid)
                .appClientUserSessionUuid(clientUserSessionUuid)
                .traceId(traceId)
                .regionId(regionId)
                .build();

        EmailInfoDto emailInfo = EmailInfoDto.builder()
                .email("duplicate@example.com")
                .verificationStatus("VERIFIED")
                .build();

        UpdateUserInternalRequest request = UpdateUserInternalRequest.builder()
                .requestContext(requestContext)
                .emailInfo(emailInfo)
                .build();

        UserProfile duplicateUser = new UserProfile();
        duplicateUser.setUserUuid(UUID.randomUUID().toString());
        when(userProfileRepository.findUserByEmail(orgUuid, "duplicate@example.com")).thenReturn(duplicateUser);

        // Act & Assert
        CompletableFuture<UpdateUserInternalRequest> result = validator.validateUniqueUser(existingUser, request);
        CompletionException exception = assertThrows(CompletionException.class, () -> result.join());
        assertTrue(exception.getCause() instanceof DuplicateResourceException);
    }
}
