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
            return cb.equal(root.get("organizationUuid"), orgUuid);
        };
    }

    public static Specification<JobProfile> withReportingManager(String reportingManagerUuid) {
        return (root, query, cb) -> {
            if (reportingManagerUuid == null) {
                return null;
            }
            return cb.equal(root.get("reportingManager"), reportingManagerUuid);
        };
    }

    public static Specification<JobProfile> withOverlappingDates(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null) {
                return null;
            }
            if (end == null) {
                return cb.isNull(root.get("endDate"));
            }
            return cb.not(cb.or(cb.lessThan(root.get("endDate"), start), cb.greaterThan(root.get("startDate"), end)));
        };
    }

    public static Specification<JobProfile> withJobProfileUuids(List<String> uuids) {
        return (root, query, cb) -> {
            if (uuids == null || uuids.isEmpty()) {
                return null;
            }
            return root.get("jobProfileUuid").in(uuids);
        };
    }

}
