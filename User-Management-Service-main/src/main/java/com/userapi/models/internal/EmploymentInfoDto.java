package com.userapi.models.internal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private JsonNode extensionsData;

}
