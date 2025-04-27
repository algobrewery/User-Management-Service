package com.userapi.models.entity;

import javax.persistence.*;

@Entity
@Table(name = "user_reportees")
public class UserReportee {

    @Id
    @Column(name = "relation_uuid")
    private String relationUuid;

    @Column(name = "organization_uuid", nullable = false)
    private String organizationUuid;

    @Column(name = "manager_user_uuid", nullable = false)
    private String managerUserUuid;

    @Column(name = "user_uuid", nullable = false)
    private String userUuid;

    @Column(name = "job_profile_uuid", nullable = false)
    private String jobProfileUuid;

    // Getters and Setters
    public String getRelationUuid() {
        return relationUuid;
    }

    public void setRelationUuid(String relationUuid) {
        this.relationUuid = relationUuid;
    }

    public String getOrganizationUuid() {
        return organizationUuid;
    }

    public void setOrganizationUuid(String organizationUuid) {
        this.organizationUuid = organizationUuid;
    }

    public String getManagerUserUuid() {
        return managerUserUuid;
    }

    public void setManagerUserUuid(String managerUserUuid) {
        this.managerUserUuid = managerUserUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getJobProfileUuid() {
        return jobProfileUuid;
    }

    public void setJobProfileUuid(String jobProfileUuid) {
        this.jobProfileUuid = jobProfileUuid;
    }
}
