package com.userapi.converters;

import com.userapi.models.external.UpdateUserRequest;
import com.userapi.models.internal.RequestContext;
import com.userapi.models.internal.UpdateUserInternalRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("UpdateUserRequestConverter")
public class UpdateUserRequestConverter
        extends ExternalRequestToInternalRequestConverter<UpdateUserRequest, UpdateUserInternalRequest> {

    private final EmailInfoConverter emailInfoConverter;
    private final EmploymentInfoConverter employmentInfoConverter;
    private final PhoneInfoConverter phoneInfoConverter;

    @Autowired
    public UpdateUserRequestConverter(
            @Qualifier("EmailInfoConverter") EmailInfoConverter emailInfoConverter,
            @Qualifier("EmploymentInfoConverter") EmploymentInfoConverter employmentInfoConverter,
            @Qualifier("PhoneInfoConverter") PhoneInfoConverter phoneInfoConverter) {
        this.emailInfoConverter = emailInfoConverter;
        this.employmentInfoConverter = employmentInfoConverter;
        this.phoneInfoConverter = phoneInfoConverter;
    }

    @Override
    protected UpdateUserInternalRequest toInternal(RequestContext rc, UpdateUserRequest external) {
        return UpdateUserInternalRequest.builder()
                .username(external.getUsername())
                .firstName(external.getFirstName())
                .lastName(external.getLastName())
                .status(external.getStatus())
                .emailInfo(emailInfoConverter.doForward(external.getEmailInfo()))
                .phoneInfo(phoneInfoConverter.doForward(external.getPhoneInfo()))
                .employmentInfo(employmentInfoConverter.doForward(external.getEmploymentInfo()))
                .requestContext(rc)
                .build();
    }
}
