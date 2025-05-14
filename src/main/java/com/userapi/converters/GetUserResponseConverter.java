package com.userapi.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.external.GetUserResponse;
import com.userapi.models.external.GetUserResponse.GetUserResponseBuilder;
import com.userapi.models.external.JobProfileInfo;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.internal.GetUserInternalRequest;
import com.userapi.models.internal.GetUserInternalResponse;
import com.userapi.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;

@Component("GetUserResponseConverter")
public class GetUserResponseConverter
        extends InternalResponseToExternalResponseConverter<GetUserInternalResponse, GetUserResponse> {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public GetUserResponseConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected GetUserResponse convert(GetUserInternalResponse internal) {
        if (isNull(internal)) {
            return null;
        }
        Optional<UserProfile> userProfileOptional = Optional.ofNullable(internal.getUserProfile());
        return GetUserResponse.builder()
                .userId(userProfileOptional.map(UserProfile::getUserUuid).orElse(null))
                .username(userProfileOptional.map(UserProfile::getUsername).orElse(null))
                .firstName(userProfileOptional.map(UserProfile::getFirstName).orElse(null))
                .middleName(userProfileOptional.map(UserProfile::getMiddleName).orElse(null))
                .lastName(userProfileOptional.map(UserProfile::getLastName).orElse(null))
                .email(userProfileOptional.map(UserProfile::getEmail).orElse(null))
                .phone(userProfileOptional.map(UserProfile::getPhone).orElse(null))
                .startDate(userProfileOptional.map(UserProfile::getStartDate).orElse(null))
                .endDate(userProfileOptional.map(UserProfile::getEndDate).orElse(null))
                .status(userProfileOptional.map(UserProfile::getStatus).orElse(null))
                .jobProfiles(buildJobProfileInfoList(internal))
                .httpStatus(HttpStatus.OK) // Example of using a static value
                .build();
    }

    private List<JobProfileInfo> buildJobProfileInfoList(GetUserInternalResponse internalResponse) {
        UserProfile userProfile = internalResponse.getUserProfile();
        Map<String, JobProfile> jobProfilesByUuid = internalResponse.getJobProfilesByUuid();
        if (isNull(userProfile) || isNull(jobProfilesByUuid)) {
            return null;
        }
        Map<String, List<String>> reporteesByJobProfileUuid = Optional.ofNullable(internalResponse.getReporteesByJobProfileUuid())
                .orElseGet(Collections::emptyMap);
        return Arrays.stream(userProfile.getJobProfileUuids())
                .map(jpUuid ->
                        buildJobProfileInfo(
                                jobProfilesByUuid.getOrDefault(jpUuid, JobProfile.builder().jobProfileUuid(jpUuid).build()),
                                reporteesByJobProfileUuid.getOrDefault(jpUuid, Collections.emptyList())))
                .toList();
    }

    private JobProfileInfo buildJobProfileInfo(JobProfile jp, List<String> reportees) {
        return JobProfileInfo.builder()
                .jobProfileUuid(jp.getJobProfileUuid())
                .jobTitle(jp.getTitle())
                .startDate(jp.getStartDate())
                .endDate(jp.getEndDate())
                .reportingManager(jp.getReportingManager())
                .reportees(reportees)
                .organizationUnit(jp.getOrganizationUnit())
                .extensionsData(readExtensionDataJson(jp.getExtensionsData()))
                .build();
    }

    private Map<String, Object> readExtensionDataJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            logger.error("Error parsing JSON: ", e);
            return new HashMap<>();
        }
    }
}
