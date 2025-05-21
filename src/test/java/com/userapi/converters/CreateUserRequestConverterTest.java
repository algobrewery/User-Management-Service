package com.userapi.converters;

import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.EmailInfo;
import com.userapi.models.external.EmploymentInfo;
import com.userapi.models.external.PhoneInfo;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.EmailInfoDto;
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.models.internal.PhoneInfoDto;
import com.userapi.models.internal.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserRequestConverterTest {

    @Mock
    private EmailInfoConverter emailInfoConverter;

    @Mock
    private PhoneInfoConverter phoneInfoConverter;

    @Mock
    private EmploymentInfoConverter employmentInfoConverter;

    @InjectMocks
    private CreateUserRequestConverter converter;

    private String orgUuid;
    private String userUuid;
    private String clientUserSessionUuid;
    private String traceId;
    private String regionId;
    private LocalDateTime startDate;
    private Map<String, Object> extensionsData;

    @BeforeEach
    void setUp() {
        orgUuid = UUID.randomUUID().toString();
        userUuid = UUID.randomUUID().toString();
        clientUserSessionUuid = UUID.randomUUID().toString();
        traceId = UUID.randomUUID().toString();
        regionId = "US";
        startDate = LocalDateTime.now();
        extensionsData = new HashMap<>();
        extensionsData.put("key1", "value1");
        extensionsData.put("key2", 123);
    }

    @Test
    void toInternal_WhenValidRequest_ShouldConvertCorrectly() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setFirstName("Test");
        request.setLastName("User");

        EmailInfo emailInfo = new EmailInfo();
        emailInfo.setEmail("test@example.com");
        emailInfo.setVerificationStatus("VERIFIED");
        request.setEmailInfo(emailInfo);

        PhoneInfo phoneInfo = new PhoneInfo();
        phoneInfo.setNumber("1234567890");
        phoneInfo.setCountryCode(1);
        phoneInfo.setVerificationStatus("VERIFIED");
        request.setPhoneInfo(phoneInfo);

        EmploymentInfo employmentInfo = new EmploymentInfo();
        employmentInfo.setJobTitle("Software Engineer");
        employmentInfo.setOrganizationUnit("Engineering");
        employmentInfo.setStartDate(startDate);
        employmentInfo.setReportingManager("manager1");
        employmentInfo.setExtensionsData(extensionsData);
        request.setEmploymentInfoList(Arrays.asList(employmentInfo));

        EmailInfoDto emailInfoInternal = EmailInfoDto.builder()
                .email("test@example.com")
                .verificationStatus("VERIFIED")
                .build();

        PhoneInfoDto phoneInfoInternal = PhoneInfoDto.builder()
                .number("1234567890")
                .countryCode(1)
                .verificationStatus("VERIFIED")
                .build();

        EmploymentInfoDto employmentInfoInternal = EmploymentInfoDto.builder()
                .jobTitle("Software Engineer")
                .organizationUnit("Engineering")
                .startDate(startDate)
                .reportingManager("manager1")
                .extensionsData(extensionsData)
                .build();

        when(emailInfoConverter.doForward(any(EmailInfo.class))).thenReturn(emailInfoInternal);
        when(phoneInfoConverter.doForward(any(PhoneInfo.class))).thenReturn(phoneInfoInternal);
        when(employmentInfoConverter.doForward(any(EmploymentInfo.class))).thenReturn(employmentInfoInternal);

        // Act
        CreateUserInternalRequest result = converter.toInternal(
                orgUuid,
                userUuid,
                clientUserSessionUuid,
                traceId,
                regionId,
                request
        );

        // Assert
        assertNotNull(result);
        assertEquals(orgUuid, result.getRequestContext().getAppOrgUuid());
        assertEquals(userUuid, result.getRequestContext().getAppUserUuid());
        assertEquals(clientUserSessionUuid, result.getRequestContext().getAppClientUserSessionUuid());
        assertEquals(traceId, result.getRequestContext().getTraceId());
        assertEquals(regionId, result.getRequestContext().getRegionId());
        assertEquals("testuser", result.getUsername());
        assertEquals("Test", result.getFirstName());
        assertEquals("User", result.getLastName());
        
        // Verify email info
        EmailInfoDto emailInfoResult = result.getEmailInfo();
        assertNotNull(emailInfoResult);
        assertEquals("test@example.com", emailInfoResult.getEmail());
        
        // Verify phone info
        PhoneInfoDto phoneInfoResult = result.getPhoneInfo();
        assertNotNull(phoneInfoResult);
        assertEquals("1234567890", phoneInfoResult.getNumber());
        assertEquals(1, phoneInfoResult.getCountryCode());

        // Verify employment info
        assertNotNull(result.getEmploymentInfoList());
        assertEquals(1, result.getEmploymentInfoList().size());
        EmploymentInfoDto employmentInfoResult = result.getEmploymentInfoList().get(0);
        assertEquals("Software Engineer", employmentInfoResult.getJobTitle());
        assertEquals("Engineering", employmentInfoResult.getOrganizationUnit());
        assertEquals(startDate, employmentInfoResult.getStartDate());
        assertEquals("manager1", employmentInfoResult.getReportingManager());
        assertEquals(extensionsData, employmentInfoResult.getExtensionsData());
    }

    @Test
    void toInternal_WhenRequestIsNull_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            converter.toInternal(orgUuid, userUuid, clientUserSessionUuid, traceId, regionId, null)
        );
    }

    @Test
    void toInternal_WhenRequiredFieldsAreMissing_ShouldThrowException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        // Missing required fields: firstName, lastName, emailInfo, phoneInfo, employmentInfoList

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            converter.toInternal(orgUuid, userUuid, clientUserSessionUuid, traceId, regionId, request)
        );
    }
}
