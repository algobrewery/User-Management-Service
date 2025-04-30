package com.userapi.models.external;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class EmploymentInfo {
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Organization unit is required")
    private String organizationUnit;

    private String reportingManager;

    @NotNull(message = "Extensions data is required")
    private Map<String, Object> extensionsData;
}
