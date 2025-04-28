package com.userapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.entity.UserReportee;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.exception.ResourceNotFoundException;
import com.userapi.models.external.*;
import com.userapi.repository.JobProfileRepository;
import com.userapi.repository.UserProfileRepository;
import com.userapi.repository.UserReporteeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfileRepository userProfileRepository;
    private final JobProfileRepository jobProfileRepository;
    private final UserReporteeRepository userReporteeRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request, String orgUuid) {
        // Check for duplicate username, email, or phone (single DB call)
        List<UserProfile> conflicts = userProfileRepository.findConflictingUsers(
                request.getUsername(),
                request.getEmailInfo().getEmail(),
                request.getPhoneInfo().getNumber()
        );

        for (UserProfile user : conflicts) {
            if (user.getUsername().equals(request.getUsername())) {
                throw new DuplicateResourceException("Username already exists");
            }
            if (user.getEmail().equals(request.getEmailInfo().getEmail())) {
                throw new DuplicateResourceException("Email already exists");
            }
            if (user.getPhone().equals(request.getPhoneInfo().getNumber())) {
                throw new DuplicateResourceException("Phone number already exists");
            }
        }

        // Create JobProfiles
        List<String> jobProfileUuids = new ArrayList<>();

        // Use the start date of the earliest job profile
        LocalDateTime earliestStartDate = request.getEmploymentInfoList().stream()
                .map(EmploymentInfo::getStartDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        for (EmploymentInfo emp : request.getEmploymentInfoList()) {
            JobProfile jobProfile = JobProfile.builder()
                    .jobProfileUuid(UUID.randomUUID().toString())
                    .organizationUuid(orgUuid)
                    .title(emp.getJobTitle())
                    .startDate(emp.getStartDate())
                    .endDate(emp.getEndDate())
                    .reportingManager(emp.getReportingManager())
                    .organizationUnit(emp.getOrganizationUnit())
                    .extensionsData(writeJson(emp.getExtensionsData()))
                    .build();
            jobProfileRepository.save(jobProfile);
            jobProfileUuids.add(jobProfile.getJobProfileUuid());
        }

        UserProfile userProfile = UserProfile.builder()
                .userUuid(UUID.randomUUID().toString())
                .organizationUuid(orgUuid)
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .startDate(earliestStartDate)
                .endDate(null)
                .jobProfileUuids(jobProfileUuids.toArray(new String[0]))
                .email(request.getEmailInfo().getEmail())
                .emailVerificationStatus(request.getEmailInfo().getVerificationStatus())
                .phone(request.getPhoneInfo().getNumber())
                .phoneCountryCode(request.getPhoneInfo().getCountryCode())
                .phoneVerificationStatus(request.getPhoneInfo().getVerificationStatus())
                .status("Active")
                .build();
        userProfileRepository.save(userProfile);

        return new CreateUserResponse(
                userProfile.getUserUuid(),
                userProfile.getUsername(),
                userProfile.getStatus(),
                "User created successfully"
        );
    }

    @Transactional(readOnly = true)
    public GetUserResponse getUser(String userId) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        GetUserResponse response = new GetUserResponse();
        response.setUserId(user.getUserUuid());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setMiddleName(user.getMiddleName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(formatPhoneNumber(user));
        response.setStartDate(user.getStartDate());
        response.setEndDate(user.getEndDate());
        response.setStatus(user.getStatus());

        // Fetch all job profiles
        List<JobProfile> jobProfiles = new ArrayList<>();
        for (String jpUuid : user.getJobProfileUuids()) {
            jobProfileRepository.findById(jpUuid).ifPresent(jobProfiles::add);
        }

        // Sort by startDate descending
        jobProfiles.sort(Comparator.comparing(JobProfile::getStartDate).reversed());

        // Current: endDate == null or latest startDate
        JobProfileDTO currentJobProfile = null;
        List<JobProfileDTO> previousJobProfiles = new ArrayList<>();

        // Find current job profile (no end date)
        JobProfile current = jobProfiles.stream()
                .filter(jp -> jp.getEndDate() == null)
                .findFirst()
                .orElse(jobProfiles.isEmpty() ? null : jobProfiles.get(0));

        if (current != null) {
            currentJobProfile = convertToJobProfileDTO(current);

            // Find reportees for current job
            List<UserReportee> reporteeRelations = userReporteeRepository.findByManagerUserUuid(user.getUserUuid());
            List<String> reporteeIds = reporteeRelations.stream()
                    .map(UserReportee::getUserUuid)
                    .collect(Collectors.toList());

            currentJobProfile.setReportees(reporteeIds);
        }

        // Find previous job profiles
        for (JobProfile jp : jobProfiles) {
            if (current == null || !jp.getJobProfileUuid().equals(current.getJobProfileUuid())) {
                JobProfileDTO dto = convertToJobProfileDTO(jp);

                // Find reportees for this past job profile
                List<UserReportee> reporteeRelations = userReporteeRepository.findByJobProfileUuid(jp.getJobProfileUuid());
                List<String> reporteeIds = reporteeRelations.stream()
                        .map(UserReportee::getUserUuid)
                        .collect(Collectors.toList());

                dto.setReportees(reporteeIds);
                previousJobProfiles.add(dto);
            }
        }

        response.setCurrentJobProfile(currentJobProfile);
        response.setPreviousJobProfiles(previousJobProfiles);

        return response;
    }

    private String formatPhoneNumber(UserProfile user) {
        return "+" + user.getPhoneCountryCode() + user.getPhone();
    }

    private JobProfileDTO convertToJobProfileDTO(JobProfile jobProfile) {
        JobProfileDTO dto = new JobProfileDTO();
        dto.setJobTitle(jobProfile.getTitle());
        dto.setStartDate(jobProfile.getStartDate());
        dto.setEndDate(jobProfile.getEndDate());
        dto.setReportingManager(jobProfile.getReportingManager());
        dto.setOrganizationUnit(jobProfile.getOrganizationUnit());
        dto.setExtensionsData(readJson(jobProfile.getExtensionsData()));
        return dto;
    }

    private static final Map<String, Function<UserProfile, Object>> fieldExtractors = Map.of(
            "userId", UserProfile::getUserUuid,
            "username", UserProfile::getUsername,
            "status", UserProfile::getStatus,
            "firstName", UserProfile::getFirstName,
            "lastName", UserProfile::getLastName,
            "email", UserProfile::getEmail,
            "phone", UserProfile::getPhone
    );

    @Transactional(readOnly = true)
    public ListUsersResponse listUsers(ListUsersRequest request, String orgUuid) {
        final List<String> baseAttributes = (request.getSelector() != null && request.getSelector().getBase_attributes() != null)
                ? request.getSelector().getBase_attributes()
                : null;

        Map<String, List<String>> filterMap = new HashMap<>();
        if (request.getFilterCriteria() != null && request.getFilterCriteria().getAttributes() != null) {
            for (ListUsersFilterCriteriaAttribute attr : request.getFilterCriteria().getAttributes()) {
                filterMap.put(attr.getName(), attr.getValues());
            }
        }

        List<UserProfile> users = userProfileRepository.findUsersWithFilters(
                orgUuid,
                filterMap.get("email"),
                filterMap.get("username"),
                filterMap.get("status"),
                filterMap.get("firstName"),
                filterMap.get("lastName"),
                filterMap.get("phone")
        );

        List<Map<String, Object>> userMaps = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            if (baseAttributes == null || baseAttributes.isEmpty()) {
                fieldExtractors.forEach((field, extractor) -> map.put(field, extractor.apply(user)));
            } else {
                for (String attr : baseAttributes) {
                    Function<UserProfile, Object> extractor = fieldExtractors.get(attr);
                    if (extractor != null) {
                        map.put(attr, extractor.apply(user));
                    }
                }
            }
            return map;
        }).collect(Collectors.toList());

        ListUsersResponse response = new ListUsersResponse();
        response.setUsers(userMaps);
        return response;
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}