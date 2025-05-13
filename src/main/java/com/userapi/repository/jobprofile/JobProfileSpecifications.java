package com.userapi.repository.jobprofile;

import com.userapi.models.entity.JobProfile;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class JobProfileSpecifications {

    public static Specification<JobProfile> withOrganizationUuid(String orgUuid) {
        return (root, query, cb) -> {
            if (orgUuid == null) {
                return null;
            }
            return cb.equal(root.get("organization_uuid"), orgUuid);
        };
    }

    public static Specification<JobProfile> withReportingManager(String reportingManagerUuid) {
        return (root, query, cb) -> {
            if (reportingManagerUuid == null) {
                return null;
            }
            return cb.equal(root.get("reporting_manager"), reportingManagerUuid);
        };
    }

    public static Specification<JobProfile> withOverlappingDates(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null) {
                return null;
            }
            if (end == null) {
                return cb.isNull(root.get("end_date"));
            }
            return cb.not(cb.or(cb.lessThan(root.get("end_date"), start), cb.greaterThan(root.get("start_date"), end)));
        };
    }

    public static Specification<JobProfile> withJobProfileUuids(List<String> uuids) {
        return (root, query, cb) -> {
            if (uuids == null || uuids.isEmpty()) {
                return null;
            }
            return root.get("job_profile_uuid").in(uuids);
        };
    }

}
