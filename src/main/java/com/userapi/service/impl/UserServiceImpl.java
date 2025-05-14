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
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import com.userapi.repository.jobprofile.JobProfileRepository;
import com.userapi.repository.jobprofile.JobProfileSpecifications;
import com.userapi.repository.userprofile.UserProfileRepository;
import com.userapi.repository.userreportee.UserReporteeRepository;
import com.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.userapi.repository.jobprofile.JobProfileSpecifications.withJobProfileUuids;
import static com.userapi.repository.jobprofile.JobProfileSpecifications.withOrganizationUuid;
import static com.userapi.repository.jobprofile.JobProfileSpecifications.withOverlappingDates;
import static com.userapi.repository.jobprofile.JobProfileSpecifications.withReportingManager;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    // Converters
    private final EmploymentInfoDtoToJobProfileConverter employmentInfoDtoToJobProfileConverter;

    // Repositories
    private final UserProfileRepository userProfileRepository;
    private final JobProfileRepository jobProfileRepository;
    private final UserReporteeRepository userReporteeRepository;

    // Utility
    private final ObjectMapper objectMapper;

    @Transactional
    public CompletableFuture<CreateUserInternalResponse> createUser(CreateUserInternalRequest request) {
        logger.info("Starting user creation for username: {}, org: {}",
                request.getUsername(), request.getRequestContext().getAppOrgUuid());
        String userUuid = UUID.randomUUID().toString();
        String orgUuid = request.getRequestContext().getAppOrgUuid();
        return validateUniqueUser(request)
                .thenCompose(this::getReportingManagersMatchingJobProfileUuids)
                .thenCompose(v -> createJobProfiles(userUuid, orgUuid, v))
                .thenCompose(v -> buildUserProfile(userUuid, request, v))
                .thenCompose(v -> CompletableFuture.completedFuture(userProfileRepository.save(v)))
                .thenApply(this::buildCreateUserInternalResponse)
                .exceptionally(this::handleCreateUserException);
    }

    private CreateUserInternalResponse buildCreateUserInternalResponse(UserProfile v) {
        return CreateUserInternalResponse.builder()
                .userId(v.getUserUuid())
                .username(v.getUsername())
                .status(v.getStatus())
                .message("User created successfully")
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .build();
    }

    private CreateUserInternalResponse handleCreateUserException(Throwable ex) {
        logger.error("Exception in creating user", ex);
        Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

        // Build error response based on exception type
        if (cause instanceof ResourceNotFoundException) {
            return CreateUserInternalResponse.builder()
                    .message("Resource not found: " + cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.ENTITY_NOT_FOUND)
                    .build();
        } else {
            return CreateUserInternalResponse.builder()
                    .message("Internal server error: " + cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private CompletableFuture<CreateUserInternalRequest> validateUniqueUser(CreateUserInternalRequest request) {
        try {
            List<UserProfile> conflicts = userProfileRepository.findUsersMatchingAny(
                    request.getRequestContext().getAppOrgUuid(),
                    request.getUsername(),
                    request.getEmailInfo().getEmail(),
                    request.getPhoneInfo().getNumber()
            );
            Set<String> matchedAttributes = new HashSet<>();
            for (UserProfile user : conflicts) {
                if (user.getUsername().equals(request.getUsername())) {
                    matchedAttributes.add("username");
                }
                if (user.getEmail().equals(request.getEmailInfo().getEmail())) {
                    matchedAttributes.add("email");
                }
                if (user.getPhone().equals(request.getPhoneInfo().getNumber())) {
                    matchedAttributes.add("phone");
                }
            }
            if (matchedAttributes.isEmpty()) {
                logger.debug("validated user is unique");
                return CompletableFuture.completedFuture(request);
            }

            String errorMessage = String.format("found existing users matching attributes: %s", matchedAttributes);
            logger.error(errorMessage);
            return CompletableFuture.failedFuture(new DuplicateResourceException(errorMessage));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Map<EmploymentInfoDto, List<JobProfile>>> getReportingManagersMatchingJobProfileUuids(
            CreateUserInternalRequest request) {
        String orgUuid = request.getRequestContext().getAppOrgUuid();
        try {
            if (request.getEmploymentInfoList().isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
            Set<String> incomingReportingManagerUuidSet = request.getEmploymentInfoList()
                    .stream()
                    .map(EmploymentInfoDto::getReportingManager)
                    .collect(Collectors.toSet());
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

            List<CompletableFuture<Pair<EmploymentInfoDto, List<JobProfile>>>> futures = request.getEmploymentInfoList()
                    .stream()
                    .map(eid ->
                            CompletableFuture.supplyAsync(()->
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
            EmploymentInfoDto employmentInfoDto,
            Map<String, UserProfile> reportingManagerProfilesMap,
            String orgUuid) {
        String reportingManagerUuid = employmentInfoDto.getReportingManager();
        List<String> reportingManagerJobProfileUuids = Arrays.asList(
                reportingManagerProfilesMap.get(reportingManagerUuid).getJobProfileUuids());
        Specification<JobProfile> spec = withOrganizationUuid(orgUuid)
                .and(withJobProfileUuids(reportingManagerJobProfileUuids))
                .and(withOverlappingDates(employmentInfoDto.getStartDate(), employmentInfoDto.getEndDate()));
        return jobProfileRepository.findAll(spec);
    }

    private CompletableFuture<List<JobProfile>> createJobProfiles(
            String userUuid,
            String orgUuid,
            Map<EmploymentInfoDto, List<JobProfile>> managerJobProfilesByEmploymentInfoDto) {
        try {
            List<CompletableFuture<JobProfile>> futures = managerJobProfilesByEmploymentInfoDto.entrySet()
                    .stream()
                    .map(entry -> CompletableFuture.supplyAsync(() ->
                                    createJobProfile(userUuid, orgUuid, entry.getKey(), entry.getValue()),
                                    executor)
                            .completeOnTimeout(null, 500, TimeUnit.MILLISECONDS))
                    .toList();

            List<JobProfile> incomingUserSavedJobProfiles = futures.stream().map(CompletableFuture::join).toList();
            List<String> savedJobProfileUuids = incomingUserSavedJobProfiles.stream()
                            .map(JobProfile::getJobProfileUuid)
                            .toList();
            logger.debug("Saved jobProfiles with uuids:{}", savedJobProfileUuids);
            return CompletableFuture.completedFuture(incomingUserSavedJobProfiles);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private JobProfile createJobProfile(
            String userUuid,
            String orgUuid,
            EmploymentInfoDto employmentInfoDto,
            List<JobProfile> reportingManagerMatchingJobProfiles) {
        String reportingManagerUuid = employmentInfoDto.getReportingManager();
        JobProfile savedJobProfile = jobProfileRepository.save(
                employmentInfoDtoToJobProfileConverter.convert(employmentInfoDto, orgUuid));
        List<UserReportee> userReporteeEntries = reportingManagerMatchingJobProfiles.stream()
                .map(jp -> UserReportee.builder()
                        .jobProfileUuid(jp.getJobProfileUuid())
                        .userUuid(userUuid)
                        .managerUserUuid(reportingManagerUuid)
                        .organizationUuid(orgUuid)
                        .relationUuid(UUID.randomUUID().toString())
                        .build())
                .toList();
        userReporteeRepository.saveAll(userReporteeEntries);
        logger.debug("Saved JobProfileUuid:{} with userReporteeEntries:{}",
                savedJobProfile.getJobProfileUuid(), userReporteeEntries);
        return savedJobProfile;
    }

    private CompletableFuture<UserProfile> buildUserProfile(
            String userUuid,
            CreateUserInternalRequest request,
            List<JobProfile> jobProfileEntityList) {
        logger.debug("Building user profile for username: {}", request.getUsername());

        // Use the start date of the earliest job profile
        LocalDateTime earliestStartDate = jobProfileEntityList.stream()
                .map(JobProfile::getStartDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        List<String> jobProfileUuids = jobProfileEntityList.stream()
                .map(JobProfile::getJobProfileUuid)
                .toList();

        return CompletableFuture.completedFuture(UserProfile.builder()
                .userUuid(userUuid)
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
                .build());
    }

    @Transactional(readOnly = true)
    public GetUserResponse getUser(String orgUUID, String userId) {
        logger.info("Fetching user details for userId: {}, org: {}", userId, orgUUID);

        try {
            UserProfile user = Optional.ofNullable(userProfileRepository.findByUserId(orgUUID, userId))
                    .orElseThrow(() -> {
                        logger.warn("User not found: {}", userId);
                        return new ResourceNotFoundException("User not found: " + userId);
                    });

            logger.debug("Found user profile, fetching job profiles");

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
            logger.debug("Found {} job profiles", jobProfiles.size());

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

            logger.info("Successfully retrieved user details for userId: {}", userId);
            return response;
        } catch (Exception e) {
            logger.error("Error fetching user details: ", e);
            throw e;
        }
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
        logger.info("Listing users for org: {}, page: {}, size: {}",
                orgUuid, request.getPage(), request.getSize());

        try {
            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getSize(),
                    Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy())
            );

            Map<String, List<String>> filters = new HashMap<>();
            if (request.getFilterCriteria() != null && request.getFilterCriteria().getAttributes() != null) {
                for (ListUsersFilterCriteriaAttribute attr : request.getFilterCriteria().getAttributes()) {
                    filters.put(attr.getName(), attr.getValues());
                }
            }
            logger.debug("Applied filters: {}", filters);

            Page<UserProfile> userPage = userProfileRepository.findUsersWithFilters(orgUuid, filters, pageable);
            logger.debug("Found {} users matching criteria", userPage.getTotalElements());

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

            logger.info("Successfully listed users, total: {}", response.getTotalElements());
            return response;
        } catch (Exception e) {
            logger.error("Error listing users: ", e);
            throw e;
        }
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

    @Transactional
    public UpdateUserResponse updateUser(String orgUuid, String userId, UpdateUserRequest request) {
        logger.info("Updating user: {} for org: {}", userId, orgUuid);

        try {
            UserProfile user = userProfileRepository.findByUserId(orgUuid, userId);
            if (user == null) {
                logger.warn("User not found for update: {}", userId);
                throw new ResourceNotFoundException("User not found: " + userId);
            }

            logger.debug("Found user, applying updates");

            // Update basic fields
            if (request.getFirstName() != null) {
                logger.debug("Updating firstName from {} to {}", user.getFirstName(), request.getFirstName());
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                logger.debug("Updating lastName from {} to {}", user.getLastName(), request.getLastName());
                user.setLastName(request.getLastName());
            }

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
                            logger.error("Invalid phone country code: {}", countryCodeStr);
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

            if (request.getStatus() != null) {
                logger.debug("Updating status from {} to {}", user.getStatus(), request.getStatus());
                user.setStatus(request.getStatus());
            }

            // Update current job profile
            UpdateUserRequest.CurrentJobProfile currentJobProfileDto = request.getCurrentJobProfile();
            if (currentJobProfileDto != null) {
                logger.debug("Updating current job profile");
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
                    logger.debug("Creating new job profile");
                    currentJobProfile = JobProfile.builder()
                            .jobProfileUuid(UUID.randomUUID().toString())
                            .organizationUuid(orgUuid)
                            .build();
                    jobProfiles.add(currentJobProfile);
                }

                if (currentJobProfileDto.getJobTitle() != null) {
                    logger.debug("Updating job title from {} to {}",
                            currentJobProfile.getTitle(), currentJobProfileDto.getJobTitle());
                    currentJobProfile.setTitle(currentJobProfileDto.getJobTitle());
                }
                if (currentJobProfileDto.getStartDate() != null) {
                    currentJobProfile.setStartDate(currentJobProfileDto.getStartDate());
                }
                currentJobProfile.setEndDate(currentJobProfileDto.getEndDate());
                if (currentJobProfileDto.getReportingManager() != null) {
                    currentJobProfile.setReportingManager(currentJobProfileDto.getReportingManager());
                }
                if (currentJobProfileDto.getOrganizationUnit() != null) {
                    currentJobProfile.setOrganizationUnit(currentJobProfileDto.getOrganizationUnit());
                }
                if (currentJobProfileDto.getExtensionsData() != null) {
                    currentJobProfile.setExtensionsData(writeJson(currentJobProfileDto.getExtensionsData()));
                }

                jobProfileRepository.save(currentJobProfile);
                logger.debug("Saved updated job profile");

                // Add new job profile UUID if not present
                if (!Arrays.asList(user.getJobProfileUuids()).contains(currentJobProfile.getJobProfileUuid())) {
                    List<String> updatedJobProfileUuids = new ArrayList<>(Arrays.asList(user.getJobProfileUuids()));
                    updatedJobProfileUuids.add(currentJobProfile.getJobProfileUuid());
                    user.setJobProfileUuids(updatedJobProfileUuids.toArray(new String[0]));
                }

                // Update reportees
                if (currentJobProfileDto.getReportees() != null) {
                    logger.debug("Updating reportees, count: {}", currentJobProfileDto.getReportees().size());
                    userReporteeRepository.deleteByJobProfileUuid(currentJobProfile.getJobProfileUuid());

                    for (String reporteeUserId : currentJobProfileDto.getReportees()) {
                        UserReportee reportee = UserReportee.builder()
                                .relationUuid(UUID.randomUUID().toString())
                                .organizationUuid(user.getOrganizationUuid())
                                .jobProfileUuid(currentJobProfile.getJobProfileUuid())
                                .managerUserUuid(user.getUserUuid())
                                .userUuid(reporteeUserId)
                                .build();
                        userReporteeRepository.save(reportee);
                    }
                }
            }

            userProfileRepository.save(user);
            logger.info("User updated successfully: {}", userId);

            return UpdateUserResponse.builder()
                    .userId(user.getUserUuid())
                    .message("User updated successfully")
                    .build();
        } catch (Exception e) {
            logger.error("Error updating user: ", e);
            throw e;
        }
    }

    @Transactional
    @Override
    public UpdateUserResponse deactivateUser(String orgUuid, String userId) {
        logger.info("Deactivating user: {} for org: {}", userId, orgUuid);

        try {
            UserProfile user = userProfileRepository.findByUserId(orgUuid, userId);
            if (user == null) {
                logger.warn("User not found for deactivation: {}", userId);
                throw new ResourceNotFoundException("User not found: " + userId);
            }

            logger.debug("Current user status: {}", user.getStatus());
            user.setStatus("Inactive");
            userProfileRepository.save(user);
            logger.info("User deactivated successfully: {}", userId);

            return UpdateUserResponse.builder()
                    .userId(user.getUserUuid())
                    .message("User deactivated successfully")
                    .status(user.getStatus())
                    .build();
        } catch (Exception e) {
            logger.error("Error deactivating user: ", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public UserHierarchyResponse getUserHierarchy(String orgUUID, String userId) {
        logger.info("Fetching user hierarchy for user: {} in org: {}", userId, orgUUID);

        try {
            UserProfile user = Optional.ofNullable(userProfileRepository.findByUserId(orgUUID, userId))
                    .orElseThrow(() -> {
                        logger.warn("User not found for hierarchy: {}", userId);
                        return new ResourceNotFoundException("User not found: " + userId);
                    });

            UserHierarchyResponse response = new UserHierarchyResponse();
            response.setUserId(user.getUserUuid());

            // Fetch reporting manager
            JobProfile currentJobProfile = getCurrentJobProfile(user);
            if (currentJobProfile != null && currentJobProfile.getReportingManager() != null) {
                logger.debug("Found reporting manager: {}", currentJobProfile.getReportingManager());
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
            logger.debug("Found {} reportee relations", reporteeRelations.size());

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
            logger.info("Successfully retrieved hierarchy for user: {}", userId);
            return response;
        } catch (Exception e) {
            logger.error("Error fetching user hierarchy: ", e);
            throw e;
        }
    }

    private JobProfile getCurrentJobProfile(UserProfile user) {
        logger.debug("Getting current job profile for user: {}", user.getUserUuid());

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

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("Error converting map to JSON: ", e);
            return "{}";
        }
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            logger.error("Error parsing JSON: ", e);
            return new HashMap<>();
        }
    }
}
