package com.userapi.converters;

import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.EmploymentInfo;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.models.internal.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    protected CreateUserInternalRequest toInternal(RequestContext rc, CreateUserRequest external) {
        if (external == null) {
            throw new IllegalArgumentException("CreateUserRequest cannot be null");
        }

        if (!StringUtils.hasText(external.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }
        if (!StringUtils.hasText(external.getFirstName())) {
            throw new IllegalArgumentException("First name is required");
        }
        if (!StringUtils.hasText(external.getLastName())) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (external.getEmailInfo() == null) {
            throw new IllegalArgumentException("Email info is required");
        }
        if (external.getPhoneInfo() == null) {
            throw new IllegalArgumentException("Phone info is required");
        }
        if (external.getEmploymentInfoList() == null || external.getEmploymentInfoList().isEmpty()) {
            throw new IllegalArgumentException("At least one employment info is required");
        }

        return CreateUserInternalRequest.builder()
                .username(external.getUsername())
                .firstName(external.getFirstName())
                .lastName(external.getLastName())
                .emailInfo(emailInfoConverter.doForward(external.getEmailInfo()))
                .phoneInfo(phoneInfoConverter.doForward(external.getPhoneInfo()))
                .employmentInfoList(convertEmploymentInfo(external.getEmploymentInfoList()))
                .requestContext(rc)
                .build();
    }

    private List<EmploymentInfoDto> convertEmploymentInfo(List<EmploymentInfo> employmentInfoList) {
        return Optional.ofNullable(employmentInfoList)
                .map(List::stream)
                .map(stream -> stream.map(employmentInfoConverter::doForward).collect(Collectors.toList()))
                .orElse(null);
    }
}
