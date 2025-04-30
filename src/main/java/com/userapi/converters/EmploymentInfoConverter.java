package com.userapi.converters;

import com.userapi.models.external.EmploymentInfo;
import com.userapi.models.internal.EmploymentInfoDto;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("EmploymentInfoConverter")
public class EmploymentInfoConverter implements BiDirectionalConverter<EmploymentInfo, EmploymentInfoDto> {

    @Override
    public EmploymentInfoDto doForward(EmploymentInfo external) {
        if (Objects.isNull(external)) {
            return null;
        }
        return EmploymentInfoDto.builder()
                .jobTitle(external.getJobTitle())
                .organizationUnit(external.getOrganizationUnit())
                .startDate(external.getStartDate())
                .endDate(external.getEndDate())
                .reportingManager(external.getReportingManager())
                .extensionsData(external.getExtensionsData())
                .build();
    }

    @Override
    public EmploymentInfo doBackward(EmploymentInfoDto internal) {
        if (Objects.isNull(internal)) {
            return null;
        }
        final EmploymentInfo external = new EmploymentInfo();
        external.setJobTitle(external.getJobTitle());
        external.setOrganizationUnit(external.getOrganizationUnit());
        external.setStartDate(external.getStartDate());
        external.setEndDate(external.getEndDate());
        external.setReportingManager(external.getReportingManager());
        external.setExtensionsData(external.getExtensionsData());
        return external;
    }
}
