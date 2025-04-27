package com.userapi.models.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "user_reportees")
@Getter
@Setter
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
