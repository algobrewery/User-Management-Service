package com.userapi.models.external;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

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

    // Getters and Setters
    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public void setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
    }

    public String getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(String reportingManager) {
        this.reportingManager = reportingManager;
    }

    public JsonNode getExtensionsData() {
        return extensionsData;
    }

    public void setExtensionsData(JsonNode extensionsData) {
        this.extensionsData = extensionsData;
    }
}