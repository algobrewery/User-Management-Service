package com.userapi.models.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
public class UserProfile {
    @Id
    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "organization_uuid", nullable = false)
    private String organizationUuid;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Type(type = "string-array")
    @Column(name = "job_profile_uuids", columnDefinition = "text[]")
    private String[] jobProfileUuids;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_verification_status")
    private String emailVerificationStatus;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "phone_country_code", nullable = false)
    private Integer phoneCountryCode;

    @Column(name = "phone_verification_status")
    private String phoneVerificationStatus;

    @Column(name = "status", nullable = false)
    private String status;
}
