package com.userapi.service.tasks;

import com.userapi.exception.ResourceNotFoundException;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.repository.jobprofile.JobProfileRepository;
import com.userapi.repository.userprofile.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReportingManagerFetcherTest {

    @InjectMocks
    private ReportingManagerFetcher reportingManagerFetcher;

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private JobProfileRepository jobProfileRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void fetchMatchingJobProfileUuids_success() {
        String orgUuid = "org1";
        EmploymentInfoDto employmentInfo = EmploymentInfoDto.builder()
                .reportingManager("manager1")
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(10))
                .jobTitle("Software Engineer")
                .organizationUnit("Development")
                .extensionsData(new HashMap<String, Object>())  // <-- fixed here
                .build();
        List<EmploymentInfoDto> employmentInfoList = Collections.singletonList(employmentInfo);

        UserProfile managerProfile = UserProfile.builder()
                .userUuid("manager1")
                .jobProfileUuids(new String[]{"job1"})
                .build();

        JobProfile jobProfile = JobProfile.builder()
                .jobProfileUuid("job1")
                .build();

        when(userProfileRepository.findByUserIdsIn(anyString(), anySet()))
                .thenReturn(Collections.singletonList(managerProfile));
        when(jobProfileRepository.findAll(any(Specification.class)))
                .thenReturn(Collections.singletonList(jobProfile));

        CompletableFuture<Map<EmploymentInfoDto, List<JobProfile>>> resultFuture =
                reportingManagerFetcher.fetchMatchingJobProfileUuids(orgUuid, employmentInfoList);

        Map<EmploymentInfoDto, List<JobProfile>> result = resultFuture.join();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(employmentInfo));
        assertEquals(1, result.get(employmentInfo).size());
        assertEquals("job1", result.get(employmentInfo).get(0).getJobProfileUuid());
    }

    @Test
    void fetchMatchingJobProfileUuids_noManagersFound() {
        String orgUuid = "org1";
        EmploymentInfoDto employmentInfo = EmploymentInfoDto.builder()
                .reportingManager("manager1")
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(10))
                .jobTitle("Software Engineer")
                .organizationUnit("Development")
                .extensionsData(new HashMap<String, Object>())  // <-- fixed here
                .build();
        List<EmploymentInfoDto> employmentInfoList = Collections.singletonList(employmentInfo);

        when(userProfileRepository.findByUserIdsIn(anyString(), anySet()))
                .thenReturn(Collections.emptyList());

        CompletableFuture<Map<EmploymentInfoDto, List<JobProfile>>> resultFuture =
                reportingManagerFetcher.fetchMatchingJobProfileUuids(orgUuid, employmentInfoList);

        // Unwrap CompletionException to check for the underlying cause
        Throwable exception = assertThrows(CompletionException.class, resultFuture::join).getCause();
        assertTrue(exception instanceof ResourceNotFoundException);
        assertEquals("Unable to find reportingManagers:[manager1]", exception.getMessage());
    }

    @Test
    void fetchMatchingJobProfileUuids_emptyEmploymentInfoList() {
        String orgUuid = "org1";
        List<EmploymentInfoDto> employmentInfoList = Collections.emptyList();

        CompletableFuture<Map<EmploymentInfoDto, List<JobProfile>>> resultFuture =
                reportingManagerFetcher.fetchMatchingJobProfileUuids(orgUuid, employmentInfoList);

        Map<EmploymentInfoDto, List<JobProfile>> result = resultFuture.join();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}