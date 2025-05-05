package com.userapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.converters.EmploymentInfoDtoToJobProfileConverter;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.exception.ResourceNotFoundException;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.entity.UserReportee;
import com.userapi.models.entity.UserStatus;
import com.userapi.models.external.*;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import com.userapi.repository.JobProfileRepository;
import com.userapi.repository.UserProfileRepository;
import com.userapi.repository.UserReporteeRepository;
import com.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Converters
    private final EmploymentInfoDtoToJobProfileConverter employmentInfoDtoToJobProfileConverter;

    // Repositories
    private final UserProfileRepository userProfileRepository;
    private final JobProfileRepository jobProfileRepository;
    private final UserReporteeRepository userReporteeRepository;

    // Utility
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateUserInternalResponse createUser(CreateUserInternalRequest request) {
        // Check for duplicate username, email, or phone (single DB call)
        List<UserProfile> conflicts = userProfileRepository.findUsersMatchingAny(
                request.getRequestContext().getAppOrgUuid(),
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

        List<JobProfile> jobProfileEntityList = employmentInfoDtoToJobProfileConverter.convertList(
                request.getEmploymentInfoList(),
                request.getRequestContext().getAppOrgUuid());
        jobProfileRepository.saveAll(jobProfileEntityList);

        UserProfile userProfile = buildUserProfile(request, jobProfileEntityList);
        userProfileRepository.save(userProfile);

        return CreateUserInternalResponse.builder()
                .userId(userProfile.getUserUuid())
                .username(userProfile.getUsername())
                .status(userProfile.getStatus())
                .message("User created successfully")
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .build();
    }

    private UserProfile buildUserProfile(CreateUserInternalRequest request,
                                         List<JobProfile> jobProfileEntityList) {
        // Use the start date of the earliest job profile
        LocalDateTime earliestStartDate = jobProfileEntityList.stream()
                .map(JobProfile::getStartDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        List<String> jobProfileUuids = jobProfileEntityList.stream()
                .map(JobProfile::getJobProfileUuid)
                .toList();

        return UserProfile.builder()
                .userUuid(UUID.randomUUID().toString())
                .organizationUuid(request.getRequestContext().getAppOrgUuid())
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .startDate(earliestStartDate)
                .jobProfileUuids(jobProfileUuids.toArray(new String[0]))
                .email(request.getEmailInfo().getEmail())
                .emailVerificationStatus(request.getEmailInfo().getVerificationStatus())
                .phone(request.getPhoneInfo().getNumber())
                .phoneCountryCode(request.getPhoneInfo().getCountryCode())
                .phoneVerificationStatus(request.getPhoneInfo().getVerificationStatus())
                .status(UserStatus.ACTIVE.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public GetUserResponse getUser(String orgUUID, String userId) {
        UserProfile user = Optional.ofNullable(userProfileRepository.findByUserId(orgUUID, userId))
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
        JobProfileInfo currentJobProfile = null;
        List<JobProfileInfo> previousJobProfiles = new ArrayList<>();

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
                JobProfileInfo dto = convertToJobProfileDTO(jp);

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

    private JobProfileInfo convertToJobProfileDTO(JobProfile jobProfile) {
        JobProfileInfo dto = new JobProfileInfo();
        dto.setJobTitle(jobProfile.getTitle());
        dto.setStartDate(jobProfile.getStartDate());
        dto.setEndDate(jobProfile.getEndDate());
        dto.setReportingManager(jobProfile.getReportingManager());
        dto.setOrganizationUnit(jobProfile.getOrganizationUnit());
        dto.setExtensionsData(readJson(jobProfile.getExtensionsData()));
        return dto;
    }

    private static final Map<String, Function<UserProfile, Object>> userProfileFieldExtractors = Map.ofEntries(
            Map.entry("userId", UserProfile::getUserUuid),
            Map.entry("username", UserProfile::getUsername),
            Map.entry("status", UserProfile::getStatus),
            Map.entry("firstName", UserProfile::getFirstName),
            Map.entry("middleName", UserProfile::getMiddleName),
            Map.entry("lastName", UserProfile::getLastName),
            Map.entry("email", UserProfile::getEmail),
            Map.entry("phone", UserProfile::getPhone),
            Map.entry("startDate", UserProfile::getStartDate),
            Map.entry("endDate", UserProfile::getEndDate)
    );

    @Transactional(readOnly = true)
    public ListUsersResponse listUsers(ListUsersRequest request, String orgUuid) {
        // Create pageable object
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy())
        );

        // Convert filter criteria to map
        Map<String, List<String>> filters = new HashMap<>();
        if (request.getFilterCriteria() != null && request.getFilterCriteria().getAttributes() != null) {
            for (ListUsersFilterCriteriaAttribute attr : request.getFilterCriteria().getAttributes()) {
                filters.put(attr.getName(), attr.getValues());
            }
        }

        // Get paginated results
        Page<UserProfile> userPage = userProfileRepository.findUsersWithFilters(orgUuid, filters, pageable);

        // Convert to response
        ListUsersResponse response = new ListUsersResponse();
        response.setUsers(userPage.getContent().stream()
                .map(user -> convertUserToMap(user, request.getSelector()))
                .collect(Collectors.toList()));

        // Set pagination metadata
        response.setTotalElements(userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());
        response.setCurrentPage(userPage.getNumber());
        response.setPageSize(userPage.getSize());
        response.setHasNext(userPage.hasNext());
        response.setHasPrevious(userPage.hasPrevious());

        return response;
    }

    private Map<String, Object> convertUserToMap(UserProfile user, ListUsersSelector selector) {
        Map<String, Object> map = new HashMap<>();
        if (selector == null || selector.getBase_attributes() == null) {
            userProfileFieldExtractors.forEach((field, extractor) -> map.put(field, extractor.apply(user)));
        } else {
            for (String attr : selector.getBase_attributes()) {
                Function<UserProfile, Object> extractor = userProfileFieldExtractors.get(attr);
                if (extractor != null) {
                    map.put(attr, extractor.apply(user));
                }
            }
        }
        return map;
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
        @Transactional
    public UpdateUserResponse updateUser(String orgUuid, String userId, UpdateUserRequest request) {
        UserProfile user = userProfileRepository.findByUserId(orgUuid, userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        // Update basic fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());

        if (request.getPhone() != null) {
            String phone = request.getPhone();
            if (phone.startsWith("+")) {
                String digits = phone.substring(1);
                if (digits.length() > 10) {
                    String countryCodeStr = digits.substring(0, digits.length() - 10);
                    try {
                        Integer countryCode = Integer.valueOf(countryCodeStr);
                        user.setPhoneCountryCode(countryCode);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid phone country code: " + countryCodeStr);
                    }
                    user.setPhone(digits.substring(digits.length() - 10));
                } else {
                    user.setPhone(digits);
                }
            } else {
                user.setPhone(phone);
            }

        }

        if (request.getStatus() != null) user.setStatus(request.getStatus());

        // Update current job profile
        UpdateUserRequest.CurrentJobProfile currentJobProfileDto = request.getCurrentJobProfile();
        if (currentJobProfileDto != null) {
            List<JobProfile> jobProfiles = new ArrayList<>();
            for (String jpUuid : user.getJobProfileUuids()) {
                jobProfileRepository.findById(jpUuid).ifPresent(jobProfiles::add);
            }
            jobProfiles.sort(Comparator.comparing(JobProfile::getStartDate).reversed());

            JobProfile currentJobProfile = jobProfiles.stream()
                    .filter(jp -> jp.getEndDate() == null)
                    .findFirst()
                    .orElse(jobProfiles.isEmpty() ? null : jobProfiles.get(0));

            if (currentJobProfile == null) {
                currentJobProfile = JobProfile.builder()
                        .jobProfileUuid(UUID.randomUUID().toString())
                        .organizationUuid(orgUuid)
                        .build();
                jobProfiles.add(currentJobProfile);
            }

            if (currentJobProfileDto.getJobTitle() != null) currentJobProfile.setTitle(currentJobProfileDto.getJobTitle());
            if (currentJobProfileDto.getStartDate() != null) currentJobProfile.setStartDate(currentJobProfileDto.getStartDate());
            currentJobProfile.setEndDate(currentJobProfileDto.getEndDate());
            if (currentJobProfileDto.getReportingManager() != null) currentJobProfile.setReportingManager(currentJobProfileDto.getReportingManager());
            if (currentJobProfileDto.getOrganizationUnit() != null) currentJobProfile.setOrganizationUnit(currentJobProfileDto.getOrganizationUnit());
            if (currentJobProfileDto.getExtensionsData() != null) currentJobProfile.setExtensionsData(writeJson(currentJobProfileDto.getExtensionsData()));

            jobProfileRepository.save(currentJobProfile);

            // Add new job profile UUID if not present
            if (!Arrays.asList(user.getJobProfileUuids()).contains(currentJobProfile.getJobProfileUuid())) {
                List<String> updatedJobProfileUuids = new ArrayList<>(Arrays.asList(user.getJobProfileUuids()));
                updatedJobProfileUuids.add(currentJobProfile.getJobProfileUuid());
                user.setJobProfileUuids(updatedJobProfileUuids.toArray(new String[0]));
            }

            // Update reportees
            if (currentJobProfileDto.getReportees() != null) {
                userReporteeRepository.deleteByJobProfileUuid(currentJobProfile.getJobProfileUuid());

                for (String reporteeUserId : currentJobProfileDto.getReportees()) {
                    UserReportee reportee = UserReportee.builder()
                            .relationUuid(UUID.randomUUID().toString())  // <-- assign UUID here
                            .organizationUuid(user.getOrganizationUuid()) // set organizationUuid as well
                            .jobProfileUuid(currentJobProfile.getJobProfileUuid())
                            .managerUserUuid(user.getUserUuid())
                            .userUuid(reporteeUserId)
                            .build();
                    userReporteeRepository.save(reportee);

                }
            }
        }

        userProfileRepository.save(user);

        return UpdateUserResponse.builder()
                .userId(user.getUserUuid())
                .message("User updated successfully")
                .build();
    }
    @Transactional
    @Override
    public UpdateUserResponse deactivateUser(String orgUuid, String userId) {
        UserProfile user = userProfileRepository.findByUserId(orgUuid, userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        user.setStatus("Inactive");
        userProfileRepository.save(user);

        return UpdateUserResponse.builder()
                .userId(user.getUserUuid())
                .message("User deactivated successfully")
                .status(user.getStatus())
                .build();
    }
    @Transactional(readOnly = true)
    public UserHierarchyResponse getUserHierarchy(String orgUUID, String userId) {
        UserProfile user = Optional.ofNullable(userProfileRepository.findByUserId(orgUUID, userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserHierarchyResponse response = new UserHierarchyResponse();
        response.setUserId(user.getUserUuid());

        // Fetch reporting manager
        JobProfile currentJobProfile = getCurrentJobProfile(user);
        if (currentJobProfile != null && currentJobProfile.getReportingManager() != null) {
            UserProfile manager = userProfileRepository.findByUserId(orgUUID, currentJobProfile.getReportingManager());
            if (manager != null) {
                UserHierarchyResponse.UserInfo managerInfo = new UserHierarchyResponse.UserInfo();
                managerInfo.setUserId(manager.getUserUuid());
                managerInfo.setName(manager.getFirstName() + " " + manager.getLastName());
                response.setReportingManager(managerInfo);
            }
        }

        // Fetch reportees
        List<UserReportee> reporteeRelations = userReporteeRepository.findByManagerUserUuid(user.getUserUuid());
        List<UserHierarchyResponse.UserInfo> reportees = reporteeRelations.stream()
                .map(rel -> {
                    UserProfile reportee = userProfileRepository.findByUserId(orgUUID, rel.getUserUuid());
                    if (reportee != null) {
                        UserHierarchyResponse.UserInfo reporteeInfo = new UserHierarchyResponse.UserInfo();
                        reporteeInfo.setUserId(reportee.getUserUuid());
                        reporteeInfo.setName(reportee.getFirstName() + " " + reportee.getLastName());
                        return reporteeInfo;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        response.setReportees(reportees);

        return response;
    }

    private JobProfile getCurrentJobProfile(UserProfile user) {
        List<JobProfile> jobProfiles = new ArrayList<>();
        for (String jpUuid : user.getJobProfileUuids()) {
            jobProfileRepository.findById(jpUuid).ifPresent(jobProfiles::add);
        }
        jobProfiles.sort(Comparator.comparing(JobProfile::getStartDate).reversed());

        return jobProfiles.stream()
                .filter(jp -> jp.getEndDate() == null)
                .findFirst()
                .orElse(jobProfiles.isEmpty() ? null : jobProfiles.get(0));
    }
}
