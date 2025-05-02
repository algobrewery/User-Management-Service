package com.userapi.converters;

import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.EmploymentInfo;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.models.internal.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("CreateUserRequestConverter")
public class CreateUserRequestConverter
        extends ExternalRequestToInternalRequestConverter<CreateUserRequest, CreateUserInternalRequest> {

    private final EmailInfoConverter emailInfoConverter;
    private final EmploymentInfoConverter employmentInfoConverter;
    private final PhoneInfoConverter phoneInfoConverter;

    @Autowired
    public CreateUserRequestConverter(
            @Qualifier("EmailInfoConverter") EmailInfoConverter emailInfoConverter,
            @Qualifier("EmploymentInfoConverter") EmploymentInfoConverter employmentInfoConverter,
            @Qualifier("PhoneInfoConverter") PhoneInfoConverter phoneInfoConverter) {
        this.emailInfoConverter = emailInfoConverter;
        this.employmentInfoConverter = employmentInfoConverter;
        this.phoneInfoConverter = phoneInfoConverter;
    }

    @Override
    protected CreateUserInternalRequest toInternal(CreateUserRequest external) {
        // Create a dummy RequestContext that will be replaced by the parent class
        RequestContext dummyContext = RequestContext.builder()
                .appOrgUuid("dummy")
                .appUserUuid("dummy")
                .appClientUserSessionUuid("dummy")
                .traceId("dummy")
                .regionId("dummy")
                .build();

        return CreateUserInternalRequest.builder()
                .username(external.getUsername())
                .firstName(external.getFirstName())
                .lastName(external.getLastName())
                .emailInfo(emailInfoConverter.doForward(external.getEmailInfo()))
                .phoneInfo(phoneInfoConverter.doForward(external.getPhoneInfo()))
                .employmentInfoList(convertEmploymentInfo(external.getEmploymentInfoList()))
                .requestContext(dummyContext)  // Add the dummy context here
                .build();
    }

    private List<EmploymentInfoDto> convertEmploymentInfo(List<EmploymentInfo> employmentInfoList) {
        return Optional.ofNullable(employmentInfoList)
                .map(List::stream)
                .map(stream -> stream.map(employmentInfoConverter::doForward).collect(Collectors.toList()))
                .orElse(null);
    }
}
