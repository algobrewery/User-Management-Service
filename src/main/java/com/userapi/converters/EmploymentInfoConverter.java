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
        external.setJobTitle(internal.getJobTitle());
        external.setOrganizationUnit(internal.getOrganizationUnit());
        external.setStartDate(internal.getStartDate());
        external.setEndDate(internal.getEndDate());
        external.setReportingManager(internal.getReportingManager());
        external.setExtensionsData(internal.getExtensionsData());
        return external;
    }
}