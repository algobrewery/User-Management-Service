package com.userapi.service.tasks;

import com.userapi.exception.ResourceNotFoundException;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.repository.jobprofile.JobProfileRepository;
import com.userapi.repository.userprofile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.userapi.repository.jobprofile.JobProfileSpecifications.withJobProfileUuids;
import static com.userapi.repository.jobprofile.JobProfileSpecifications.withOrganizationUuid;
import static com.userapi.repository.jobprofile.JobProfileSpecifications.withOverlappingDates;

@Component("ReportingManagerFetcher")
@RequiredArgsConstructor
public class ReportingManagerFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ReportingManagerFetcher.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    // Repositories
    private final UserProfileRepository userProfileRepository;
    private final JobProfileRepository jobProfileRepository;

    public CompletableFuture<Map<EmploymentInfoDto, List<JobProfile>>> fetchMatchingJobProfileUuids(
            String orgUuid,
            List<EmploymentInfoDto> reporteeEmploymentInfoList) {
        try {
            if (reporteeEmploymentInfoList.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
            Set<String> incomingReportingManagerUuidSet = reporteeEmploymentInfoList.stream()
                    .map(EmploymentInfoDto::getReportingManager)
                    .filter(uuid -> uuid != null && !uuid.isEmpty())
                    .collect(Collectors.toSet());
            
            if (incomingReportingManagerUuidSet.isEmpty()) {
                logger.debug("No reporting managers specified, returning empty map");
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
            
            logger.debug("Attempting to fetch reportingManagers with uuids:{}", incomingReportingManagerUuidSet);

            List<UserProfile> reportingManagerProfiles = userProfileRepository.findByUserIdsIn(
                    orgUuid,
                    incomingReportingManagerUuidSet);
            Map<String, UserProfile> reportingManagerProfilesMap = reportingManagerProfiles.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            UserProfile::getUserUuid,
                            Function.identity()));
            logger.debug("Found user profiles for reportingManagers with uuids:{}", reportingManagerProfilesMap.keySet());

            incomingReportingManagerUuidSet.removeAll(reportingManagerProfilesMap.keySet());
            if (!incomingReportingManagerUuidSet.isEmpty()) {
                String errMessage = String.format("Unable to find reportingManagers:%s", incomingReportingManagerUuidSet);
                logger.error(errMessage);
                return CompletableFuture.failedFuture(new ResourceNotFoundException(errMessage));
            }

            List<CompletableFuture<Pair<EmploymentInfoDto, List<JobProfile>>>> futures = reporteeEmploymentInfoList.stream()
                    .map(eid ->
                            CompletableFuture.supplyAsync(() ->
                                                    Pair.of(
                                                            eid,
                                                            findMatchingReportingManagerJobProfiles(
                                                                    eid,
                                                                    reportingManagerProfilesMap,
                                                                    orgUuid)
                                                    ),
                                            executor)
                                    .completeOnTimeout(
                                            Pair.of(eid, Collections.emptyList()),
                                            500,
                                            TimeUnit.MILLISECONDS
                                    ))
                    .toList();
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v ->
                            futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<JobProfile> findMatchingReportingManagerJobProfiles(
            EmploymentInfoDto reporteeEmploymentInfoDto,
            Map<String, UserProfile> reportingManagerProfilesMap,
            String orgUuid) {
        String reportingManagerUuid = reporteeEmploymentInfoDto.getReportingManager();
        List<String> reportingManagerJobProfileUuids = Arrays.asList(
                reportingManagerProfilesMap.get(reportingManagerUuid).getJobProfileUuids());
        Specification<JobProfile> spec = withOrganizationUuid(orgUuid)
                .and(withJobProfileUuids(reportingManagerJobProfileUuids))
                .and(withOverlappingDates(reporteeEmploymentInfoDto.getStartDate(), reporteeEmploymentInfoDto.getEndDate()));
        return jobProfileRepository.findAll(spec);
    }
}
