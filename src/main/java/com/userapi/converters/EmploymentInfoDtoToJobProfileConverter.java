package com.userapi.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.internal.EmploymentInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component("EmploymentInfoDtoToJobProfileConverter")
public class EmploymentInfoDtoToJobProfileConverter {
    private static final Logger logger = LoggerFactory.getLogger(EmploymentInfoDtoToJobProfileConverter.class);


    private final ObjectMapper objectMapper;

    @Autowired
    public EmploymentInfoDtoToJobProfileConverter(
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a list of EmploymentInfoDto objects to a list of JobProfile entities.
     *
     * @param employmentInfoList List of employment information DTOs
     * @param organizationUuid   Organization UUID to associate with the job profiles
     * @return List of JobProfile entities (not yet persisted)
     */
    public List<JobProfile> convertList(List<EmploymentInfoDto> employmentInfoList, String organizationUuid) {
        logger.debug("Converting {} employment info records for org: {}",
                employmentInfoList != null ? employmentInfoList.size() : 0, organizationUuid);

        if (Objects.isNull(employmentInfoList)) {
            return null;
        }
        return employmentInfoList.stream()
                .map(x -> convert(x, organizationUuid))
                .toList();
    }

    public JobProfile convert(EmploymentInfoDto employmentInfo, String organizationUuid) {
        logger.debug("Converting employment info for org: {}, job title: {}",
                organizationUuid, employmentInfo.getJobTitle());

        if (Objects.isNull(employmentInfo)) {
            return null;
        }

        return JobProfile.builder()
                .jobProfileUuid(UUID.randomUUID().toString())
                .organizationUuid(organizationUuid)
                .title(employmentInfo.getJobTitle())
                .startDate(employmentInfo.getStartDate())
                .endDate(employmentInfo.getEndDate())
                .reportingManager(employmentInfo.getReportingManager())
                .organizationUnit(employmentInfo.getOrganizationUnit())
                .extensionsData(writeJson(employmentInfo.getExtensionsData()))
                .build();
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("Error converting map to JSON: ", e);
            return "{}";
        }
    }
}
