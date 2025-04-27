package com.userapi.models.external;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public class JobProfileInfo {
    private String jobTitle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reportingManager;
    private List<String> reportees;
    private String organizationUnit;
    private JsonNode extensionsData;

    // Getters and Setters
    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

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

    public String getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(String reportingManager) {
        this.reportingManager = reportingManager;
    }

    public List<String> getReportees() {
        return reportees;
    }

    public void setReportees(List<String> reportees) {
        this.reportees = reportees;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public void setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
    }

    public JsonNode getExtensionsData() {
        return extensionsData;
    }

    public void setExtensionsData(JsonNode extensionsData) {
        this.extensionsData = extensionsData;
    }
}