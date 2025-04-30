package com.userapi.models.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
@Table(name = "job_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
