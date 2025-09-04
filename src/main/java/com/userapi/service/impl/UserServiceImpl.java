package com.userapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.converters.EmploymentInfoDtoToJobProfileConverter;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.exception.ResourceNotFoundException;
import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.entity.UserReportee;
import com.userapi.models.entity.UserStatus;
import com.userapi.models.external.ListUsersFilterCriteriaAttribute;
import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.external.ListUsersSelector;
import com.userapi.models.external.UpdateUserResponse;
import com.userapi.models.external.UserHierarchyResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.internal.EmploymentInfoDto;
import com.userapi.models.internal.GetUserInternalRequest;
import com.userapi.models.internal.GetUserInternalResponse;
import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import com.userapi.models.internal.UpdateUserInternalRequest;
import com.userapi.models.internal.UpdateUserInternalResponse;
import com.userapi.repository.jobprofile.JobProfileRepository;
import com.userapi.repository.jobprofile.JobProfileSpecifications;
import com.userapi.repository.userprofile.UserProfileRepository;
import com.userapi.repository.userreportee.UserReporteeRepository;
import com.userapi.service.UserService;
import com.userapi.service.tasks.ReportingManagerFetcher;
import com.userapi.service.tasks.UpdateUserInternalRequestValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

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
    private final ReportingManagerFetcher reportingManagerFetcher;
    private final UpdateUserInternalRequestValidator updateUserInternalRequestValidator;
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
        } else if (cause instanceof DuplicateResourceException) {
            return CreateUserInternalResponse.builder()
                    .message(cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.DUPLICATE_USER)
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
        return reportingManagerFetcher.fetchMatchingJobProfileUuids(orgUuid, request.getEmploymentInfoList());
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
    public CompletableFuture<GetUserInternalResponse> getUser(GetUserInternalRequest request) {
        String orgUUID = request.getRequestContext().getAppOrgUuid();
        String userId = request.getUserId();
        logger.info("Fetching user details for userId: {}, org: {}", userId, orgUUID);

        return findByUserId(orgUUID, userId)
                .thenCompose(this::populateJobProfilesByUuid)
                .thenCompose(this::populateReporteesByJobProfileUuid)
                .exceptionally(this::handleGetUserException);
    }

    private GetUserInternalResponse handleGetUserException(Throwable ex) {
        logger.error("Exception in creating user", ex);
        Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

        // Build error response based on exception type
        if (cause instanceof ResourceNotFoundException) {
            return GetUserInternalResponse.builder()
                    .message("Resource not found: " + cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.ENTITY_NOT_FOUND)
                    .build();
        } else {
            return GetUserInternalResponse.builder()
                    .message("Internal server error: " + cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private CompletableFuture<GetUserInternalResponse> findByUserId(String orgUuid, String userId) {
        try {
            UserProfile userProfile = userProfileRepository.findByUserId(orgUuid, userId);
            if (isNull(userProfile)) {
                return CompletableFuture.failedFuture(new ResourceNotFoundException("User not found: " + userId));
            }
            return CompletableFuture.completedFuture(
                    GetUserInternalResponse.builder()
                            .userProfile(userProfile)
                            .responseResult(ResponseResult.SUCCESS)
                            .responseReasonCode(ResponseReasonCode.SUCCESS)
                            .message("fetched userprofile successfully")
                            .build());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<GetUserInternalResponse> populateJobProfilesByUuid(GetUserInternalResponse getUserInternalResponse) {
        UserProfile userProfile = getUserInternalResponse.getUserProfile();
        Set<String> jobProfileUuidSet = new HashSet<>(Arrays.asList(userProfile.getJobProfileUuids()));
        logger.debug("populateJobProfilesByUuid fetch jobProfiles for uuids:{}", jobProfileUuidSet);
        try {
            Specification<JobProfile> spec = JobProfileSpecifications.withOrganizationUuid(userProfile.getOrganizationUuid())
                    .and(JobProfileSpecifications.withJobProfileUuids(new LinkedList<>(jobProfileUuidSet)));
            Map<String, JobProfile> jobProfilesByUuid = jobProfileRepository.findAll(spec).stream()
                    .collect(Collectors.toUnmodifiableMap(JobProfile::getJobProfileUuid, Function.identity()));
            getUserInternalResponse.setJobProfilesByUuid(jobProfilesByUuid);

            jobProfileUuidSet.removeAll(jobProfilesByUuid.keySet());
            if (!jobProfileUuidSet.isEmpty()) {
                String errMsg = String.format("unable to fetch jobProfiles for uuids:%s", jobProfileUuidSet);
                logger.error(errMsg);
                return CompletableFuture.failedFuture(new ResourceNotFoundException(errMsg));
            }
            return CompletableFuture.completedFuture(getUserInternalResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<GetUserInternalResponse> populateReporteesByJobProfileUuid(GetUserInternalResponse getUserInternalResponse) {
        try {
            List<CompletableFuture<Pair<String, List<String>>>> futures = getUserInternalResponse.getJobProfilesByUuid()
                    .entrySet()
                    .stream()
                    .map(entry -> CompletableFuture.supplyAsync(() ->
                                            Pair.of(
                                                    entry.getKey(),
                                                    userReporteeRepository.findByJobProfileUuid(
                                                                    entry.getValue().getOrganizationUuid(),
                                                                    entry.getValue().getJobProfileUuid())
                                                            .stream()
                                                            .map(UserReportee::getUserUuid)
                                                            .toList()),
                                    executor)
                            .completeOnTimeout(
                                    Pair.of(entry.getKey(), Collections.emptyList()),
                                    500,
                                    TimeUnit.MILLISECONDS))
                    .toList();
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v ->
                            futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)))
                    .thenApply(m -> {
                        getUserInternalResponse.setReporteesByJobProfileUuid(m);
                        return getUserInternalResponse;
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
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
    public CompletableFuture<UpdateUserInternalResponse> updateUser(String userId, UpdateUserInternalRequest request) {
        logger.info("Updating user: {} with request: {}", userId, request);
        String orgUuid = request.getRequestContext().getAppOrgUuid();
        final UserProfile user;
        try {
            user = userProfileRepository.findByUserId(orgUuid, userId);
            if (isNull(user)) {
                logger.warn("User not found for update: {}", userId);
                return CompletableFuture.completedFuture(UpdateUserInternalResponse.builder()
                        .message("User not found: " + userId)
                        .responseReasonCode(ResponseReasonCode.ENTITY_NOT_FOUND)
                        .responseResult(ResponseResult.FAILURE)
                        .build());
            }
        } catch (Exception e) {
            return CompletableFuture.completedFuture(handleUpdateUserException(e));
        }
        return updateUserInternalRequestValidator.validateUniqueUser(user, request)
                .thenCompose(req ->
                        reportingManagerFetcher.fetchMatchingJobProfileUuids(
                                orgUuid,
                                Optional.ofNullable(req.getEmploymentInfoList())
                                        .orElseGet(Collections::emptyList)))
                .thenCompose(v -> createJobProfiles(userId, orgUuid, v))
                .thenApply(v -> v.stream().map(JobProfile::getJobProfileUuid).toList())
                .thenApply(jobProfileUuidsToAdd -> {
                    if (!jobProfileUuidsToAdd.isEmpty()) {
                        List<String> updatedJobProfileUuids = new ArrayList<>(jobProfileUuidsToAdd);
                        updatedJobProfileUuids.addAll(Arrays.asList(user.getJobProfileUuids()));
                        String[] updatedJobProfileUuidsArray = updatedJobProfileUuids.toArray(new String[0]);
                        user.setJobProfileUuids(updatedJobProfileUuidsArray);
                    }
                    return user;
                })
                .thenApply(u -> {
                    if (!isNull(request.getUsername())) {
                        u.setUsername(request.getUsername());
                    }
                    if (!isNull(request.getFirstName())) {
                        u.setFirstName(request.getFirstName());
                    }
                    if (!isNull(request.getMiddleName())) {
                        u.setMiddleName(request.getMiddleName());
                    }
                    if (!isNull(request.getLastName())) {
                        u.setLastName(request.getLastName());
                    }
                    if (!isNull(request.getPhoneInfo())) {
                        u.setPhone(request.getPhoneInfo().getNumber());
                        u.setPhoneCountryCode(request.getPhoneInfo().getCountryCode());
                        u.setPhoneVerificationStatus(request.getPhoneInfo().getVerificationStatus());
                    }
                    if (!isNull(request.getEmailInfo())) {
                        u.setEmail(request.getEmailInfo().getEmail());
                        u.setEmailVerificationStatus(request.getEmailInfo().getVerificationStatus());
                    }
                    if (!isNull(request.getStatus())) {
                        u.setStatus(UserStatus.valueOf(request.getStatus()).getName());
                    }
                    return u;
                })
                .thenCompose(u -> CompletableFuture.completedFuture(userProfileRepository.save(u)))
                .thenCompose(this::buildUpdateUserInternalResponse)
                .exceptionally(this::handleUpdateUserException);
    }

    private CompletableFuture<UpdateUserInternalResponse> buildUpdateUserInternalResponse(UserProfile userProfile) {
        return CompletableFuture.completedFuture(UpdateUserInternalResponse.builder()
                .responseResult(ResponseResult.SUCCESS)
                .responseReasonCode(ResponseReasonCode.SUCCESS)
                .message("Updated user successfully")
                .status(userProfile.getStatus())
                .build());
    }

    private UpdateUserInternalResponse handleUpdateUserException(Throwable ex) {
        logger.error("Exception in updating user", ex);
        Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

        // Build error response based on exception type
        if (cause instanceof ResourceNotFoundException) {
            return UpdateUserInternalResponse.builder()
                    .message("Resource not found: " + cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.ENTITY_NOT_FOUND)
                    .build();
        } else {
            return UpdateUserInternalResponse.builder()
                    .message("Internal server error: " + cause.getMessage())
                    .responseResult(ResponseResult.FAILURE)
                    .responseReasonCode(ResponseReasonCode.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @Transactional
    @Override
    public CompletableFuture<UpdateUserInternalResponse> deactivateUser(String orgUuid, String userId) {
        logger.info("Deactivating user: {} for org: {}", userId, orgUuid);
        final UserProfile user;
        try {
            user = userProfileRepository.findByUserId(orgUuid, userId);
            if (user == null) {
                logger.warn("User not found for deactivation: {}", userId);
                throw new ResourceNotFoundException("User not found: " + userId);
            }
        } catch (Exception e) {
            return CompletableFuture.completedFuture(handleUpdateUserException(e));
        }

        logger.debug("Current user status: {}", user.getStatus());
        return CompletableFuture.completedFuture(user)
                        .thenApply(u -> {
                            u.setStatus(UserStatus.INACTIVE.getName());
                            return u;
                        })
                .thenApply(userProfileRepository::save)
                .thenCompose(this::buildUpdateUserInternalResponse)
                .exceptionally(this::handleUpdateUserException);
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
            List<UserReportee> reporteeRelations = userReporteeRepository.findByManagerUserUuid(orgUUID, user.getUserUuid());
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
