package com.userapi.converters;

import com.userapi.models.entity.VerificationStatus;
import com.userapi.models.external.EmailInfo;
import com.userapi.models.internal.EmailInfoDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.userapi.TestConstants.*;

class EmailInfoConverterTest {

    private final EmailInfoConverter converter = new EmailInfoConverter();

    @Test
    void doForward_nullInput_returnsNull() {
        assertNull(converter.doForward(null));
    }

    @Test
    void doForward_validInput_convertsCorrectly() {
        EmailInfo external = new EmailInfo();
        external.setEmail(TEST_EMAIL);
        external.setVerificationStatus(VERIFICATION_STATUS_VERIFIED.toString());

        EmailInfoDto dto = converter.doForward(external);

        assertNotNull(dto);
        assertEquals(TEST_EMAIL, dto.getEmail());
        assertEquals(VERIFICATION_STATUS_VERIFIED.toString(), dto.getVerificationStatus());
    }

    @Test
    void doBackward_nullInput_returnsNull() {
        assertNull(converter.doBackward(null));
    }

    @Test
    void doBackward_validInput_convertsCorrectly() {
        EmailInfoDto dto = EmailInfoDto.builder()
                .email(TEST_EMAIL)
                .verificationStatus(VERIFICATION_STATUS_UNVERIFIED.toString())
                .build();

        EmailInfo external = converter.doBackward(dto);

        assertNotNull(external);
        assertEquals(TEST_EMAIL, external.getEmail());
        assertEquals(VERIFICATION_STATUS_UNVERIFIED.toString(), external.getVerificationStatus());
    }
}
