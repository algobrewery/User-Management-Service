package com.userapi.models.external;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmploymentInfo {
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Organization unit is required")
    private String organizationUnit;

    private String reportingManager;

    private JsonNode extensionsData;

}