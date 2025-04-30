package com.userapi.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_reportees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
