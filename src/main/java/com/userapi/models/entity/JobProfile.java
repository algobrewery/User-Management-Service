package com.userapi.models.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_profiles")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JobProfile {

    @Id
    @Column(name = "job_profile_uuid")
    private String jobProfileUuid;

    @Column(name = "organization_uuid", nullable = false)
    private String organizationUuid;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "reporting_manager")
    private String reportingManager;

    @Column(name = "organization_unit")
    private String organizationUnit;

    @Type(type = "jsonb")
    @Column(name = "extensions_data", columnDefinition = "jsonb")
    private String extensionsData;

    // Getters and Setters
    public String getJobProfileUuid() {
        return jobProfileUuid;
    }

    public void setJobProfileUuid(String jobProfileUuid) {
        this.jobProfileUuid = jobProfileUuid;
    }

    public String getOrganizationUuid() {
        return organizationUuid;
    }

    public void setOrganizationUuid(String organizationUuid) {
        this.organizationUuid = organizationUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public void setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
    }

    public String getExtensionsData() {
        return extensionsData;
    }

    public void setExtensionsData(String extensionsData) {
        this.extensionsData = extensionsData;
    }
}