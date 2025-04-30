package com.userapi.models.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Getter
@Setter
public class EmploymentInfoDto {

    @NonNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NonNull
    private String jobTitle;

    @NonNull
    private String organizationUnit;

    @NonNull
    private String reportingManager;

    @NonNull
    private Map<String, Object> extensionsData;

}
