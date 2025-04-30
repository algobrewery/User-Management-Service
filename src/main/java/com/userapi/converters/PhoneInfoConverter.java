package com.userapi.converters;

import com.userapi.models.external.PhoneInfo;
import com.userapi.models.internal.PhoneInfoDto;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("PhoneInfoConverter")
public class PhoneInfoConverter implements BiDirectionalConverter<PhoneInfo, PhoneInfoDto> {

    @Override
    public PhoneInfoDto doForward(PhoneInfo external) {
        if (Objects.isNull(external)) {
            return null;
        }
        return PhoneInfoDto.builder()
                .number(external.getNumber())
                .countryCode(external.getCountryCode())
                .verificationStatus(external.getVerificationStatus())
                .build();
    }

    @Override
    public PhoneInfo doBackward(PhoneInfoDto internal) {
        if (Objects.isNull(internal)) {
            return null;
        }
        final PhoneInfo external = new PhoneInfo();
        external.setNumber(external.getNumber());
        external.setCountryCode(external.getCountryCode());
        external.setVerificationStatus(external.getVerificationStatus());
        return external;
    }
}
