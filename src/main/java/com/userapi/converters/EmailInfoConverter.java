package com.userapi.converters;

import com.userapi.models.external.EmailInfo;
import com.userapi.models.internal.EmailInfoDto;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("EmailInfoConverter")
public class EmailInfoConverter implements BiDirectionalConverter<EmailInfo, EmailInfoDto> {
    @Override
    public EmailInfoDto doForward(EmailInfo external) {
        if (Objects.isNull(external)) {
            return null;
        }
        return EmailInfoDto.builder()
                .email(external.getEmail())
                .verificationStatus(external.getVerificationStatus())
                .build();
    }

    @Override
    public EmailInfo doBackward(EmailInfoDto internal) {
        if (Objects.isNull(internal)) {
            return null;
        }
        final EmailInfo external = new EmailInfo();
        external.setEmail(internal.getEmail());
        external.setVerificationStatus(internal.getVerificationStatus());
        return external;
    }
}
