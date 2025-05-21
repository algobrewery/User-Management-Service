package com.userapi.converters;

import com.userapi.models.external.EmailInfo;
import com.userapi.models.internal.EmailInfoDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailInfoConverterTest {

    private final EmailInfoConverter converter = new EmailInfoConverter();

    @Test
    void doForward_nullInput_returnsNull() {
        assertNull(converter.doForward(null));
    }

    @Test
    void doForward_validInput_convertsCorrectly() {
        EmailInfo external = new EmailInfo();
        external.setEmail("test@example.com");
        external.setVerificationStatus("VERIFIED");

        EmailInfoDto dto = converter.doForward(external);

        assertNotNull(dto);
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("VERIFIED", dto.getVerificationStatus());
    }

    @Test
    void doBackward_nullInput_returnsNull() {
        assertNull(converter.doBackward(null));
    }

    @Test
    void doBackward_validInput_convertsCorrectly() {
        EmailInfoDto dto = EmailInfoDto.builder()
                .email("user@example.com")
                .verificationStatus("UNVERIFIED")
                .build();

        EmailInfo external = converter.doBackward(dto);

        assertNotNull(external);
        assertEquals("user@example.com", external.getEmail());
        assertEquals("UNVERIFIED", external.getVerificationStatus());
    }
}
